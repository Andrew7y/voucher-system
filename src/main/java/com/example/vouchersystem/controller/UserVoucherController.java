package com.example.vouchersystem.controller;

import com.example.vouchersystem.domain.dto.ClaimRequest;
import com.example.vouchersystem.service.UserVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class UserVoucherController {
    private final UserVoucherService userVoucherService;

    @PostMapping("/claim")
    public ResponseEntity<Map<String, String>> claimVoucher(
            @Valid @RequestBody ClaimRequest claimRequest
    ){
        String message = userVoucherService.claimVoucher(claimRequest);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", message
        ));
    }
}
