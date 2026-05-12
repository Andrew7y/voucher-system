package com.example.vouchersystem.domain.dto;

import com.example.vouchersystem.domain.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCacheDto{
    private CampaignStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
