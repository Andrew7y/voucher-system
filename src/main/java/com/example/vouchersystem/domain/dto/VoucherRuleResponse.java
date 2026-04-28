package com.example.vouchersystem.domain.dto;

import com.example.vouchersystem.domain.entity.DiscountType;

import java.math.BigDecimal;
import java.util.Map;

public record VoucherRuleResponse(
        Long id,
        Integer totalQuota,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderVal,
        Map<String, Object> conditions
) {}
