package com.nexamarket.stock.config;

import com.nexamarket.stock.application.StockMutationLock;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class StockLockConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "stock.lock.type", havingValue = "redisson", matchIfMissing = true)
    RedissonClient redissonClient(@Value("${spring.data.redis.host}") String host,
                                  @Value("${spring.data.redis.port}") int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(name = "stock.lock.type", havingValue = "redisson", matchIfMissing = true)
    StockMutationLock redissonStockMutationLock(RedissonClient redissonClient) {
        return new StockMutationLock() {
            @Override
            public <T> T execute(Long variantId, java.util.function.Supplier<T> action) {
                RLock lock = redissonClient.getLock("stock:variant:" + variantId);
                boolean acquired = false;
                try {
                    acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                    if (!acquired) {
                        throw new IllegalStateException("Stok kilidi zamanında alınamadı.");
                    }
                    return action.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Stok kilidi beklenirken işlem kesildi.", exception);
                } finally {
                    if (acquired && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "stock.lock.type", havingValue = "database")
    StockMutationLock databaseStockMutationLock() {
        return new StockMutationLock() {
            @Override
            public <T> T execute(Long variantId, java.util.function.Supplier<T> action) {
                return action.get();
            }
        };
    }
}
