package com.example.vouchersystem.controller;

import com.example.vouchersystem.domain.dto.ApplyVoucherRequest;
import com.example.vouchersystem.domain.dto.ClaimRequest;
import com.example.vouchersystem.service.UserVoucherService;
import com.example.vouchersystem.service.VoucherCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class UserVoucherController {
    private final UserVoucherService userVoucherService;
    private final VoucherCalculationService voucherCalculationService;

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

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyVoucher(
            @RequestBody ApplyVoucherRequest applyVoucherRequest
    ){
        BigDecimal discount = voucherCalculationService.applyVoucher(
                applyVoucherRequest.userId(),
                applyVoucherRequest.userVoucherId(),
                applyVoucherRequest.orderId(),
                applyVoucherRequest.orderTotalValue()
        );
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "orser_id", applyVoucherRequest.orderId(),
                "applied_discount", discount,
                "message", "Voucher applied successfully"
        ));
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<Map<String, Object>> refundVoucher(
            @PathVariable String orderId
    ){
        voucherCalculationService.refundVoucher(orderId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "order_id", orderId,
                "message", "Voucher refunded to user wallet successfully"
        ));
    }
}
