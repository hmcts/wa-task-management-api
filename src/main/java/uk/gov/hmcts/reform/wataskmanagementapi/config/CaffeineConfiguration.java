package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineConfiguration {

    @Value("${caffeine.timeout.duration}")
    private Integer cacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.timeout.unit}')}")
    private TimeUnit cacheDurationUnit;

    @Value("${caffeine.task-type.timeout.duration}")
    private Integer taskTypeCacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.task-type.timeout.unit}')}")
    private TimeUnit taskTypeCacheDurationUnit;

    @Value("${caffeine.calendar.timeout.duration}")
    private Integer calendarCacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.calendar.timeout.unit}')}")
    private TimeUnit calendarCacheDurationUnit;

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
    @Primary
    public CacheManager cacheManager(Caffeine<Object, Object> caffeineConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeineConfig);
        return caffeineCacheManager;
    }

    @Bean
    public Caffeine<Object, Object> taskTypeCaffeineConfig(Ticker ticker) {
        return Caffeine.newBuilder()
            .expireAfterWrite(taskTypeCacheDuration, taskTypeCacheDurationUnit)
            .ticker(ticker);
    }

    @Bean
    public CacheManager taskTypeCacheManager(Caffeine<Object, Object> taskTypeCaffeineConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(taskTypeCaffeineConfig);
        caffeineCacheManager.setCacheNames(List.of("task_types", "task_types_dmn"));
        return caffeineCacheManager;
    }

    @Bean
    public Caffeine<Object, Object> calendarCaffeineConfig(Ticker ticker) {
        return Caffeine.newBuilder()
            .expireAfterWrite(calendarCacheDuration, calendarCacheDurationUnit)
            .ticker(ticker);
    }

    @Bean
    public CacheManager calendarCacheManager(Caffeine<Object, Object> calendarCaffeineConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(calendarCaffeineConfig);
        caffeineCacheManager.setCacheNames(List.of("calendar_cache"));
        return caffeineCacheManager;
    }


}
