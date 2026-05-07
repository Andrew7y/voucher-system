package com.example.vouchersystem.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_vouchers",
        indexes = {
                @Index(name = "idx_order_vouchers_order_id", columnList = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id", nullable = false)
    private UserVoucher userVoucher;

    @Column(name = "applied_discount", nullable = false, precision = 19, scale = 4)
    private BigDecimal appliedDiscount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
