package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.VoucherRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRuleRepository extends JpaRepository<VoucherRule, Long>, JpaSpecificationExecutor<VoucherRule> {
    List<VoucherRule> findByCampaignId(Long campaignId);
}
