package com.example.vouchersystem.domain.dto;

import java.math.BigDecimal;

public record ApplyVoucherRequest(
        Long userId,
        Long userVoucherId,
        String orderId,
        BigDecimal orderTotalValue
) {}
