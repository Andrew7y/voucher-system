package com.example.vouchersystem.controller;

import com.example.vouchersystem.domain.dto.CampaignCreateRequest;
import com.example.vouchersystem.domain.dto.CampaignResponse;
import com.example.vouchersystem.domain.entity.CampaignStatus;
import com.example.vouchersystem.service.CampaignAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {
    private final CampaignAdminService campaignAdminService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CampaignCreateRequest campaignCreateRequest
    ){
        CampaignResponse response = campaignAdminService.createCampaign(campaignCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(campaignAdminService.getCampaignById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CampaignResponse>> searchCampaigns(
            @RequestParam(required = false) String title,
            @RequestParam(required = false)CampaignStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ){
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                campaignAdminService.searchCampaigns(title, status, pageRequest)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignCreateRequest request
    ){
        return ResponseEntity.ok(
                campaignAdminService.updateCampaign(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelCampaign(@PathVariable Long id){
        campaignAdminService.cancelCampaign(id);
        return ResponseEntity.noContent().build();
    }
}
