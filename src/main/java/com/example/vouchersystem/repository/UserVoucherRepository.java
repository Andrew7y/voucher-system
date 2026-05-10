package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.UserVoucher;
import com.example.vouchersystem.domain.entity.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserIdAndStatus(Long userId, UserVoucherStatus status);

    boolean existsByUserIdAndVoucherRuleId(Long userId, Long ruleId);

    Optional<UserVoucher> findByIdAndUserIdAndStatus(Long id, Long userId, UserVoucherStatus status);

    @Query("""
           SELECT uv FROM UserVoucher uv
           JOIN FETCH uv.voucherRule vr
           JOIN FETCH vr.campaign
           WHERE uv.id = :id AND uv.userId = :userId
           """)
    Optional<UserVoucher> findByIdAndUserIdWithRule(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserVoucher uv SET uv.status = 'USED', uv.usedAt = :usedAt " +
            "WHERE uv.id = :id AND uv.status = 'UNUSED'")
    int markAsUsedIfUnused(@Param("id") Long id, @Param("usedAt")LocalDateTime usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserVoucher uv SET uv.status = 'UNUSED', " +
            "uv.usedAt = null WHERE uv.id = :id AND uv.status = 'USED'")
    int markAsUnusedIfUsed(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE UserVoucher uv SET uv.status = 'EXPIRED'
            WHERE uv.status = 'UNUSED'
            AND uv.voucherRule.id IN (
                SELECT vr.id FROM VoucherRule vr
                JOIN vr.campaign c
                WHERE c.endAt < :now OR c.status = 'EXPIRED'
            )
    """)
    int expireUnusedVouchers(@Param("now") LocalDateTime now);
}
