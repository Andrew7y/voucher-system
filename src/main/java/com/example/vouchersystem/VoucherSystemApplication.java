package com.example.vouchersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class VoucherSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoucherSystemApplication.class, args);
    }

}
