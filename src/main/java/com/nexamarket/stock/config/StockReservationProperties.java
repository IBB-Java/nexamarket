package com.nexamarket.stock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "stock.reservation")
public class StockReservationProperties {

    private Duration duration = Duration.ofMinutes(10);

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
