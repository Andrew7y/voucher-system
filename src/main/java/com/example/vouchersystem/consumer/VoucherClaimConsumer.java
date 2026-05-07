package com.example.vouchersystem.consumer;

import com.example.vouchersystem.config.RabbitMQConfig;
import com.example.vouchersystem.domain.dto.VoucherClaimMessage;
import com.example.vouchersystem.service.worker.VoucherWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherClaimConsumer {
    private final VoucherWorkerService voucherWorkerService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CLAIM)
    public void handleVoucherClaim(VoucherClaimMessage message){
        voucherWorkerService.processVoucherClaim(message);
    }
}
