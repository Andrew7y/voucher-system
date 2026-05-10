package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {
    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.voucherRules WHERE c.id = :id")
    Optional<Campaign> findByIdWithRules(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Campaign c SET c.status = 'EXPIRED' WHERE c.endAt < :now AND c.status = 'ACTIVE'")
    int expireActiveCampaigns(@Param("now")LocalDateTime now);
}
