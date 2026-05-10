package com.example.vouchersystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    // ==========================================
    // Constants: (Main Queue)
    // ==========================================
    public static final String EXCHANGE_VOUCHER = "voucher.exchange";
    public static final String QUEUE_CLAIM = "voucher.claim.queue";
    public static final String ROUTING_KEY_CLAIM = "voucher.claim.routing";

    // ==========================================
    // Constants: (Dead Letter Queue)
    // ==========================================
    public static final String DLX_EXCHANGE = "voucher.dlx";
    public static final String DLQ_CLAIM = "voucher.claim.dlq";
    public static final String DLQ_ROUNTING_KEY = "voucher.claim.dlq.routing";

    @Bean
    public DirectExchange deadLetterExchange(){
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue(){
        return new Queue(DLQ_CLAIM, true);
    }

    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange
    ){
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUNTING_KEY);
    }

    @Bean
    public Queue claimQueue(){
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-daed-letter-routing-key", DLQ_ROUNTING_KEY);

        return new Queue(
                QUEUE_CLAIM,
                true,
                false,
                false,
                args
        );
    }

    @Bean
    public DirectExchange voucherExchange(){
        return new DirectExchange(EXCHANGE_VOUCHER);
    }

    @Bean
    public Binding claimBinding(
            Queue claimQueue,
            DirectExchange voucherExchange
    ){
        return BindingBuilder.bind(claimQueue)
                .to(voucherExchange)
                .with(ROUTING_KEY_CLAIM);
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }
}
