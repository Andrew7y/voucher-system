package com.example.vouchersystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_VOUCHER = "voucher.exchange";
    public static final String QUEUE_CLAIM = "voucher.claim.queue";
    public static final String ROUTING_KEY_CLAIM = "voucher.claim.routing";

    @Bean
    public Queue claimQueue(){
        return new Queue(QUEUE_CLAIM, true);
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
}
