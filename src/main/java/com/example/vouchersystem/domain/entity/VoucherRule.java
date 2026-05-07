package com.example.vouchersystem.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "voucher_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "total_quota", nullable = false)
    private Integer totalQuota;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "max_discount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_val", nullable = false, precision = 19, scale = 4)
    private BigDecimal minOrderVal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> conditions = new HashMap<>();

    public VoucherRule addCondition(String key, Object value){
        this.conditions.put(key, value);
        return this;
    }

    public <T> Optional<T> getCondition(String key, Class<T> type){
        Object value = this.conditions.get(key);
        if(value == null) return Optional.empty();
        if(type.isInstance(value)) return Optional.of(type.cast(value));
        return Optional.empty();
    }

}
