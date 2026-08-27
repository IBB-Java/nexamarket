package com.nexamarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class NexaMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexaMarketApplication.class, args);
    }
}
