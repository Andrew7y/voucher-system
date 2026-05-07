package com.example.vouchersystem.service;

import com.example.vouchersystem.constant.RedisKeyConst;
import com.example.vouchersystem.domain.dto.*;
import com.example.vouchersystem.domain.entity.Campaign;
import com.example.vouchersystem.domain.entity.CampaignStatus;
import com.example.vouchersystem.domain.entity.DiscountType;
import com.example.vouchersystem.domain.entity.VoucherRule;
import com.example.vouchersystem.event.CacheSyncEvent;
import com.example.vouchersystem.exception.BusinessRuleException;
import com.example.vouchersystem.exception.ResourceNotFoundException;
import com.example.vouchersystem.repository.CampaignRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAdminService {

    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request){
        log.info("Creating new campaign: {}", request.title());

        validateCampaignRules(request);

        Campaign campaign = Campaign.builder()
                .title(request.title())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(CampaignStatus.DRAFT)
                .build();

        for (VoucherRuleCreateRequest ruleDto : request.voucherRules()){
            if (ruleDto.maxDiscount().compareTo(ruleDto.minOrderVal()) > 0) {
                throw new BusinessRuleException(
                        "Maximum discount cannot be greater than minimum order value"
                );
            }
            
            VoucherRule rule = VoucherRule.builder()
                    .totalQuota(ruleDto.totalQuota())
                    .discountType(ruleDto.discountType())
                    .discountValue(ruleDto.discountValue())
                    .maxDiscount(ruleDto.maxDiscount())
                    .minOrderVal(ruleDto.minOrderVal())
                    .conditions(ruleDto.conditions() != null ? ruleDto.conditions() : new HashMap<>())
                    .build();

            campaign.addVoucherRule(rule);
        }

        Campaign savedCampaign = campaignRepository.save(campaign);
        log.info("Successfully created campaign with ID: {}", savedCampaign.getId());

        publishCacheEvent(savedCampaign, CacheSyncEvent.Action.UPSERT);

        return mapToResponse(savedCampaign);
    }

    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignCreateRequest request){
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if(campaign.getStatus() == CampaignStatus.EXPIRED){
            throw new BusinessRuleException(
                    "Cannot update expired campaign"
            );
        }

        if(campaign.getStatus() == CampaignStatus.ACTIVE){
            log.info("Updating ACTIVE campaign ID: {}", id);
            campaign.setTitle(request.title());
            campaign.setEndAt(request.endAt());
        }else{
            campaign.setTitle(request.title());
            campaign.setStartAt(request.startAt());
            campaign.setEndAt(request.endAt());
        }

        Campaign updated = campaignRepository.save(campaign);

        publishCacheEvent(updated, CacheSyncEvent.Action.UPSERT);

        return mapToResponse(campaign);
    }

    @Transactional
    public void cancelCampaign(Long id){
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if(campaign.getStatus() == CampaignStatus.EXPIRED){
            throw new BusinessRuleException("Campaign already expired");
        }

        campaign.setStatus(CampaignStatus.EXPIRED);
        campaignRepository.save(campaign);
        log.info("Campaign ID: {} has been cancelled (marked as EXPIRED)", id);

        publishCacheEvent(campaign, CacheSyncEvent.Action.DELETE);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id){
        Campaign campaign = campaignRepository.findByIdWithRules(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        return mapToResponse(campaign);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> searchCampaigns(String title, CampaignStatus status, Pageable pageable){
        Specification<Campaign> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(title != null && !title.isBlank()){
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if(status != null){
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return campaignRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    // =========================
    // Helper Function
    // =========================

    private void validateCampaignRules(CampaignCreateRequest request){
        if(request.endAt().isBefore(request.startAt())){
            throw new BusinessRuleException(
                    "Campaign end time must be after start time"
            );
        }

        for(VoucherRuleCreateRequest rule : request.voucherRules()){
            if(rule.discountType() == DiscountType.PERCENTAGE){
                if(rule.maxDiscount() == null || rule.maxDiscount().doubleValue() <= 0){
                    throw new BusinessRuleException(
                            "Percentage discount must have a maximum discount value"
                    );
                }

                if(rule.discountValue().doubleValue() > 100){
                    throw new BusinessRuleException(
                            "Percentage discount cannot exceed 100%"
                    );
                }
            }
        }
    }

    private CampaignResponse mapToResponse(Campaign campaign){
        List<VoucherRuleResponse> ruleResponse = campaign.getVoucherRules().stream()
                .map(rule -> new VoucherRuleResponse(
                        rule.getId(),
                        rule.getTotalQuota(),
                        rule.getDiscountType(),
                        rule.getDiscountValue(),
                        rule.getMaxDiscount(),
                        rule.getMinOrderVal(),
                        rule.getConditions()
                )).toList();

        return new CampaignResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getStartAt(),
                campaign.getEndAt(),
                campaign.getStatus(),
                ruleResponse
        );
    }

    private void publishCacheEvent(Campaign campaign, CacheSyncEvent.Action action) {
        List<VoucherRule> rules = campaign.getVoucherRules();

        if (rules == null || rules.isEmpty()) return;

        CampaignCacheDto cacheDto = new CampaignCacheDto(
                campaign.getStatus(),
                campaign.getStartAt(),
                campaign.getEndAt()
        );

        Map<String, Integer> quotaToSync = new HashMap<>(rules.size());
        Map<String, CampaignCacheDto> infoToSync = new HashMap<>(rules.size());

        for (VoucherRule rule : rules) {
            quotaToSync.put(RedisKeyConst.getQuotaKey(rule.getId()), rule.getTotalQuota());
            infoToSync.put(RedisKeyConst.getInfoKey(rule.getId()), cacheDto);
        }

        eventPublisher.publishEvent(new CacheSyncEvent(
                action,
                quotaToSync,
                infoToSync,
                campaign.getId(),
                campaign.getEndAt()
        ));
    }
}
