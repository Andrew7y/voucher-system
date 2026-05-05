package com.example.vouchersystem.service;

import com.example.vouchersystem.config.RabbitMQConfig;
import com.example.vouchersystem.constant.RedisKeyConst;
import com.example.vouchersystem.domain.dto.CampaignCacheDto;
import com.example.vouchersystem.domain.dto.ClaimRequest;
import com.example.vouchersystem.domain.dto.VoucherClaimMessage;
import com.example.vouchersystem.domain.entity.CampaignStatus;
import com.example.vouchersystem.exception.BusinessRuleException;
import com.example.vouchersystem.exception.ResourceNotFoundException;
import com.example.vouchersystem.exception.SystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVoucherService {
    private final RedisService redisService;
    private final RabbitTemplate rabbitTemplate;

    public String claimVoucher(ClaimRequest claimRequest){
        log.info("User ID: {} is attempting to claim Voucher Rule ID: {}",
                claimRequest.userId(), claimRequest.ruleId()
        );

        validateCampaignEligibilityFromCache(claimRequest.ruleId());

        String quotaKey = RedisKeyConst.getQuotaKey(claimRequest.ruleId());
        String claimedKey = RedisKeyConst.getClaimedUsersKey(claimRequest.ruleId());
        String userIdStr = String.valueOf(claimRequest.userId());

        long result = redisService.executeClaimScript(
                quotaKey,
                claimedKey,
                userIdStr
        );

        if(result == 1){
            log.info("SUCCESS (Redis): User ID: {} claimed Voucher Rule ID: {}",
                    claimRequest.userId(), claimRequest.ruleId());

            VoucherClaimMessage message = new VoucherClaimMessage(
                    claimRequest.userId(),
                    claimRequest.ruleId(),
                    LocalDateTime.now()
            );

            try{
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_VOUCHER,
                        RabbitMQConfig.ROUTING_KEY_CLAIM,
                        message
                );

                log.debug("SUCCESS (RabbitMQ): Claim message published for User ID: {}",
                        claimRequest.userId());

                return "Voucher claimed successfully! It will appear in your wallet shortly.";
            }catch (AmqpException e){
                log.error("CRITICAL FAILURE: RabbitMQ is down! Executing rollback for User ID: {}",
                        claimRequest.userId(), e);
                redisService.rollbackClaimAtomically(quotaKey, claimedKey, userIdStr);

                throw new SystemException("The system is currently experiencing hign traffic." +
                        " Please trry again in a few minutes.");
            }
        }else if(result == 0){
            log.warn("FAILED: Voucher Rule ID: {} is sold out", claimRequest.ruleId());
            throw new BusinessRuleException("Sorry, this voucher is fully redeemed.");
        }else if(result == -2){
            log.warn("FAILED: User ID: {} already claimed Voucher Rule ID: {}",
                    claimRequest.userId(), claimRequest.ruleId());
            throw new BusinessRuleException("You have already claimed this voucher.");
        }else {
            log.error("FAILED: Voucher Rule ID: {} is not found in Redis", claimRequest.ruleId());
            throw new ResourceNotFoundException("Voucher campaign not found or has expired.");
        }
    }

    // ==========================================
    // Helper Method
    // ==========================================
    private void validateCampaignEligibilityFromCache(Long ruleId) {
        String infoKey = RedisKeyConst.getInfoKey(ruleId);

        Object cacheObj = redisService.getValue(infoKey);
        if(cacheObj == null){
            log.warn("Campaign info not found in cache for ruleId: {}", ruleId);
            throw new ResourceNotFoundException("Campaign not found or has expired.");
        }

        CampaignCacheDto info = (CampaignCacheDto) cacheObj;
        LocalDateTime now = LocalDateTime.now();

        if(info.status() == CampaignStatus.EXPIRED){
            throw new BusinessRuleException("This campaign has expired.");
        }
        if(info.status() == CampaignStatus.DRAFT){
            throw new BusinessRuleException("This campaign is currently inactive.");
        }

        if(now.isBefore(info.startAt())){
            throw new BusinessRuleException("This campaign has not started yet.");
        }
        if(now.isAfter(info.endAt())){
            throw new BusinessRuleException("This campaign has ended.");
        }
    }
}
