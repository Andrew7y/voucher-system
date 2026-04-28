package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {
    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.voucherRules WHERE c.id = :id")
    Optional<Campaign> findByIdWithRules(@Param("id") Long id);
}
