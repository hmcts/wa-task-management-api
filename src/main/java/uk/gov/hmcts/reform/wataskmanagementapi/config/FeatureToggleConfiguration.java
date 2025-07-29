package uk.gov.hmcts.reform.wataskmanagementapi.config;


import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class FeatureToggleConfiguration {

    @Value("${launchDarkly.sdkKey}")
    private String sdkKey;

    @Value("${launchDarkly.connectionTimeout}")
    private Integer connectionTimeout;

    @Value("${launchDarkly.socketTimeout}")
    private Integer socketTimeout;

    @Bean
    public LDConfig ldConfig() {
        log.info("SDK key {}, connection timeout {}", sdkKey, connectionTimeout);
        return new LDConfig.Builder()
            .http(Components
                .httpConfiguration()
                      .connectTimeout(Duration.ofMillis(connectionTimeout))
                      .socketTimeout(Duration.ofMillis(socketTimeout))
            )
            .build();
    }

    @Bean
    public LDClientInterface ldClient(LDConfig ldConfig) {
        log.info("SDK key", sdkKey);
        return new LDClient(sdkKey, ldConfig);
    }

}
