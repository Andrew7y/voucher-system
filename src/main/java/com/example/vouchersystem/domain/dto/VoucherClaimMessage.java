package com.example.vouchersystem.domain.dto;

import java.time.LocalDateTime;

public record VoucherClaimMessage(
        Long userId,
        Long ruleId,
        LocalDateTime claimedAt
) {}
