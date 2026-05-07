package com.example.vouchersystem.service.worker;

import com.example.vouchersystem.domain.dto.VoucherClaimMessage;
import com.example.vouchersystem.domain.entity.UserVoucher;
import com.example.vouchersystem.domain.entity.UserVoucherStatus;
import com.example.vouchersystem.domain.entity.VoucherRule;
import com.example.vouchersystem.repository.UserVoucherRepository;
import com.example.vouchersystem.repository.VoucherRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherWorkerService {
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRuleRepository voucherRuleRepository;

    @Transactional
    public void processVoucherClaim(VoucherClaimMessage message){
        log.debug("Processing DB record for User ID: {}, Rule ID: {}",
                message.userId(), message.ruleId());
        try{
            VoucherRule ruleRef = voucherRuleRepository.getReferenceById(message.ruleId());

            UserVoucher userVoucher = UserVoucher.builder()
                    .userId(message.userId())
                    .voucherRule(ruleRef)
                    .status(UserVoucherStatus.UNUSED)
                    .claimedAt(message.claimedAt())
                    .build();

            userVoucherRepository.saveAndFlush(userVoucher);

            log.info("Successfully saved voucher for User ID: {} (Rule ID: {})",
                    message.userId(), message.ruleId());
        }catch (DataIntegrityViolationException e){
            boolean isDuplicate = userVoucherRepository.existsByUserIdAndVoucherRuleId(
                    message.userId(), message.ruleId()
            );

            if(isDuplicate){
                log.warn("Idempotency Check: User ID: {} already has Rule ID: {}. Skipping duplicate message.",
                        message.userId(), message.ruleId());
            }else{
                log.error("CRITICAL DATA ERROR: Foreign Key constraint failed for User ID:{}, Rule ID: {}.",
                        message.userId(), message.ruleId(), e);
                throw e;
            }
        }catch (Exception e){
            log.error("Unexpected error while saving voucher for User ID: {}", message.userId());
            throw e;
        }
    }
}
