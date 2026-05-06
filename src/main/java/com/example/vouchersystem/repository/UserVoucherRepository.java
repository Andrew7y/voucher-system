package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.UserVoucher;
import com.example.vouchersystem.domain.entity.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserIdAndStatus(Long userId, UserVoucherStatus status);

    boolean existsByUserIdAndVoucherRuleId(Long userId, Long ruleId);

    Optional<UserVoucher> findByIdAndUserIdAndStatus(Long id, Long userId, UserVoucherStatus status);
}
