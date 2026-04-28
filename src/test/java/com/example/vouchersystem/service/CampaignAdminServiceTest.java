package com.example.vouchersystem.service;

import com.example.vouchersystem.domain.dto.CampaignCreateRequest;
import com.example.vouchersystem.domain.dto.CampaignResponse;
import com.example.vouchersystem.domain.dto.VoucherRuleCreateRequest;
import com.example.vouchersystem.domain.entity.Campaign;
import com.example.vouchersystem.domain.entity.CampaignStatus;
import com.example.vouchersystem.domain.entity.DiscountType;
import com.example.vouchersystem.exception.BusinessRuleException;
import com.example.vouchersystem.exception.ResourceNotFoundException;
import com.example.vouchersystem.repository.CampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CampaignAdminServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignAdminService campaignAdminService;

    @Test
    @DisplayName("Should create campaign successfully when valid request is provided")
    void createCampaign_Success(){
        LocalDateTime now = LocalDateTime.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Summer Sale",
                now.plusDays(1),
                now.plusDays(10),
                new ArrayList<>()
        );

        Campaign savedCampaign = Campaign.builder()
                .id(1L)
                .title("Summer Sale")
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(CampaignStatus.DRAFT)
                .voucherRules(new ArrayList<>())
                .build();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

        CampaignResponse response = campaignAdminService.createCampaign(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Summer Sale");
        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);

        verify(campaignRepository, times(1))
                .save(any(Campaign.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when end date is before start date")
    void createCampaign_ThrowException_WhenEndAtBeforeStartAt(){
        LocalDateTime now = LocalDateTime.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Error Campaign",
                now.plusDays(5),
                now.plusDays(1),
                new ArrayList<>()
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> campaignAdminService.createCampaign(request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Campaign end time must be after start time");

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    @DisplayName("Should throw exception when percentage discount has no max discount")
    void createCampaign_ThrowsException_WhenPercentageHasNoMaxDiscount(){
        VoucherRuleCreateRequest badRule = new VoucherRuleCreateRequest(
                100,
                DiscountType.PERCENTAGE,
                new BigDecimal(20),
                null,
                new BigDecimal(200),
                new HashMap<>()
        );

        LocalDateTime now = LocalDateTime.now();
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Test",
                now.plusDays(1),
                now.plusDays(2),
                List.of(badRule)
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> campaignAdminService.createCampaign(request)
        );
        assertThat(exception.getMessage())
                .isEqualTo("Percentage discount must have a maximum discount value");
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when campaign ID does not exist")
    void getCampaignById_ThrowsException_WhenNotFound(){
        when(campaignRepository.findByIdWithRules(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> campaignAdminService.getCampaignById(999L)
        );
    }

    @Test
    @DisplayName("Should throw exception when trying to update an EXPIRED campaign")
    void updateCampaign_ThrowsException_WhenCampaignExpired(){
        Campaign expiredCampaign = new Campaign();
        expiredCampaign.setId(1L);
        expiredCampaign.setStatus(CampaignStatus.EXPIRED);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(expiredCampaign));

        CampaignCreateRequest request = new CampaignCreateRequest(
                "New Title",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                new ArrayList<>()
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> campaignAdminService.updateCampaign(1L, request)
        );
        assertThat(exception.getMessage()).isEqualTo("Cannot update expired campaign");
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully cancel campaign by setting status to EXPIRED")
    void cancelCampaign_Success(){
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setStatus(CampaignStatus.ACTIVE);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        campaignAdminService.cancelCampaign(1L);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.EXPIRED);
        verify(campaignRepository, times(1)).save(campaign);
    }
}
