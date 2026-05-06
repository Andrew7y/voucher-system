package com.example.vouchersystem.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_voucher",
        indexes = {
                @Index(name = "idx_user_vouchers_user_status", columnList = "user_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_unique_user_rule", columnNames = {"user_id", "rule_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private VoucherRule voucherRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserVoucherStatus status;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
