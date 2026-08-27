package com.nexamarket.stock.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(StockReservationProperties.class)
public class StockConfiguration {

    @Bean
    public Clock stockClock() {
        return Clock.systemUTC();
    }
}
