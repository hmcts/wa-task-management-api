package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineConfiguration {

    public static final String CACHE_NAME = "CaffeineCache";

    @Value("${caffeine.timeout.duration}")
    private Integer cacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.timeout.unit}')}")
    private TimeUnit cacheDurationUnit;

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig(Ticker ticker) {
        return Caffeine.newBuilder()
            .expireAfterWrite(cacheDuration, cacheDurationUnit)
            .ticker(ticker);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeineConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeineConfig);
        return caffeineCacheManager;
    }
}
