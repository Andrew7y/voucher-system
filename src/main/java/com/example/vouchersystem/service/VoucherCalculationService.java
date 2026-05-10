package com.example.vouchersystem.service;

import com.example.vouchersystem.domain.entity.*;
import com.example.vouchersystem.exception.BusinessRuleException;
import com.example.vouchersystem.exception.ResourceNotFoundException;
import com.example.vouchersystem.repository.OrderVoucherRepository;
import com.example.vouchersystem.repository.UserVoucherRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherCalculationService {
    private final UserVoucherRepository userVoucherRepository;
    private final OrderVoucherRepository orderVoucherRepository;
    private final ObservationRegistry observationRegistry;

    @Transactional
    @Observed(name = "business.voucher.apply", contextualName = "apply-discount-checkout")
    public BigDecimal applyVoucher(
            Long userId,
            Long userVoucherId,
            String orderId,
            BigDecimal orderTotalValue
    ){
        Observation currentObservation = observationRegistry.getCurrentObservation();
        if(currentObservation != null){
            currentObservation
                    .highCardinalityKeyValue("order.id", orderId)
                    .highCardinalityKeyValue("user_voucher.id", String.valueOf(userVoucherId))
                    .lowCardinalityKeyValue("user.id", String.valueOf(userId));
        }

        log.info("Attempting to apply User Voucher ID: {} for Order ID: {}",
                userVoucherId, orderId);

        Optional<OrderVoucher> existingOrderVoucher = orderVoucherRepository
                .findByOrderIdAndUserVoucherId(orderId, userVoucherId);
        if(existingOrderVoucher.isPresent()){
            log.info("Idempotency match: Order ID: {} already applied User Voucher ID: {}. Returning existing discount.",
                    orderId, userVoucherId);
            return existingOrderVoucher.get().getAppliedDiscount();
        }

        UserVoucher userVoucher = userVoucherRepository.findByIdAndUserIdWithRule(
                userVoucherId, userId
        ).orElseThrow(() -> new ResourceNotFoundException("Voucher not found or access denied."));

        if(userVoucher.getStatus() != UserVoucherStatus.UNUSED){
            throw new BusinessRuleException("This voucher has already been used or is expired.");
        }

        VoucherRule rule = userVoucher.getVoucherRule();
        Campaign campaign = rule.getCampaign();
        LocalDateTime now = LocalDateTime.now();

        if(!campaign.isActive()){
            throw new BusinessRuleException("This campaign is no longer active.");
        }

        if(orderTotalValue.compareTo(rule.getMinOrderVal()) < 0){
            if(currentObservation != null){
                currentObservation.lowCardinalityKeyValue("error.business_reason", "MIN_ORDER_NOT_MET");
            }
            throw new BusinessRuleException("Minimum order value is " + rule.getMinOrderVal());
        }

        BigDecimal appliedDiscount;

        if(!rule.getConditions().isEmpty()){
            appliedDiscount = calculateDiscountWithCondition(
                    rule, orderTotalValue
            );
        }else {
            appliedDiscount = calculateDiscountWithOutCondition(
                    rule, orderTotalValue
            );
        }

        int updateRows = userVoucherRepository.markAsUsedIfUnused(
                userVoucher.getId(), now
        );

        if(updateRows == 0){
            log.error("Double-spending attempt detected for User Voucher ID: {}", userVoucher.getId());
            throw new BusinessRuleException("This voucher is currently being processed or already used.");
        }

        OrderVoucher orderVoucher = OrderVoucher.builder()
                .orderId(orderId)
                .userVoucher(userVoucher)
                .appliedDiscount(appliedDiscount)
                .createdAt(now)
                .build();

        orderVoucherRepository.save(orderVoucher);
        log.info("Successfully applied discount of {} to Order ID:{}",
                appliedDiscount, orderId);
        return appliedDiscount;
    }

    @Transactional
    @Observed(name = "business.voucher.refund", contextualName = "refund-voucher-payment-failed")
    public void refundVoucher(String orderId){
        if(observationRegistry.getCurrentObservation() != null){
            observationRegistry.getCurrentObservation().highCardinalityKeyValue("order.id", orderId);
        }
        log.info("Initiating voucher refund process for Order ID: {}", orderId);

        Optional<OrderVoucher> orderVoucherOpt = orderVoucherRepository.findByOrderId(orderId);
        if(orderVoucherOpt.isEmpty()){
            log.info("No voucher found for Order ID: {}. Refund process skipped.", orderId);
            return;
        }
        OrderVoucher orderVoucher = orderVoucherOpt.get();
        UserVoucher userVoucher = orderVoucher.getUserVoucher();

        int updateRows = userVoucherRepository.markAsUnusedIfUsed(
                userVoucher.getId()
        );
        if(updateRows == 0){
            log.warn("User Voucher ID: {} is alrready unsed.", userVoucher.getId());
        }

        orderVoucherRepository.delete(orderVoucher);
        log.info("Successfully refunded User Voucher ID: {} for Order ID: {}",
                userVoucher.getId(), orderId);
    }

    // ==========================================
    // Helper Method
    // ==========================================
    private BigDecimal calculateDiscountWithOutCondition(
            VoucherRule rule, BigDecimal orderTotalValue
    ){
        BigDecimal discount;

        if(rule.getDiscountType() == DiscountType.FIXED_AMOUNT){
            discount = rule.getMaxDiscount();
        }else {
            discount = orderTotalValue
                    .multiply(rule.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            if (discount.compareTo(rule.getMaxDiscount()) > 0) {
                discount = rule.getMaxDiscount();
            }
        }

        if (discount.compareTo(orderTotalValue) > 0) {
            return orderTotalValue;
        }
        return discount;
    }

    private BigDecimal calculateDiscountWithCondition(
            VoucherRule rule, BigDecimal orderTotalValue
    ){
        // Todo 1. Check condition 2. Calculate discount
        return BigDecimal.ZERO;
    }
}
