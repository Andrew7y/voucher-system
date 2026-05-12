package com.example.vouchersystem.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaigns_status_end_at", columnList = "status, endAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VoucherRule> voucherRules = new ArrayList<>();

    // Business Logic: Is the campaign active?
    public boolean isActive(){
        LocalDateTime now = LocalDateTime.now();
        return status == CampaignStatus.ACTIVE &&
                (now.isAfter(startAt) || now.isEqual(startAt)) &&
                now.isBefore(endAt);
    }

    // Helper Methods for manage the bidirectional relationship
    public void addVoucherRule(VoucherRule rule){
        voucherRules.add(rule);
        rule.setCampaign(this);
    }
}
