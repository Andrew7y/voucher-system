package com.example.vouchersystem.domain.dto;

import com.example.vouchersystem.domain.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;

public record VoucherRuleCreateRequest(
        @NotNull(message = "Total quota is required")
        @Min(value = 1, message = "Quota must be at least 1")
        Integer totalQuota,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @PositiveOrZero(message = "Discount value cannot be positive")
        BigDecimal discountValue,

        @PositiveOrZero(message = "Max discount cannot be negative")
        BigDecimal maxDiscount,

        @NotNull(message = "Minimum order value is required")
        @PositiveOrZero(message = "Minimum order value cannot be negative")
        BigDecimal minOrderVal,

        Map<String, Object> conditions
) {}
