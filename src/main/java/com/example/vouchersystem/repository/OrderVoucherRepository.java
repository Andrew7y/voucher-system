package com.example.vouchersystem.repository;

import com.example.vouchersystem.domain.entity.OrderVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderVoucherRepository extends JpaRepository<OrderVoucher, Long> {
    Optional<OrderVoucher> findByOrderIdAndUserVoucherId(String orderId, Long userVoucherId);

    Optional<OrderVoucher> findByOrderId(String orderId);
}
