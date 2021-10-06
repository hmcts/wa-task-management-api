package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
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

    @Value("${caffeine.timeout.duration}")
    private Integer cacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.timeout.unit}')}")
    private TimeUnit cacheDurationUnit;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(cacheDuration, cacheDurationUnit));
        return caffeineCacheManager;
    }
}
