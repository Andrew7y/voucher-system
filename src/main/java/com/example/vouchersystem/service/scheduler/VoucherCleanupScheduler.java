package com.example.vouchersystem.service.scheduler;

import com.example.vouchersystem.repository.CampaignRepository;
import com.example.vouchersystem.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherCleanupScheduler {
    private final CampaignRepository campaignRepository;
    private final UserVoucherRepository userVoucherRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredResources(){
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting scheduled cleanup job at {}",now);

        try{
            int expiredCampaigns = campaignRepository.expireActiveCampaigns(now);
            if(expiredCampaigns > 0){
                log.info("Update {} campaigns to EXPIRED", expiredCampaigns);
            }

            int expiredVouchers = userVoucherRepository.expireUnusedVouchers(now);
            if(expiredVouchers > 0){
                log.info("Update {} vouchers to EXPIRED", expiredVouchers);
            }
        }catch (Exception e){
            log.error("Error occurred during cleanup job", e);
        }
    }
}
