package uk.gov.hmcts.reform.wataskmanagementapi.config.executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("!functional & !local")
@SuppressWarnings("PMD.DoNotUseThreads")
public class ExecutorServiceConfig {

    @Bean("sensitiveTaskEventLogsExecutorService")
    public ExecutorService createSensitiveTaskEventLogsExecutorService() {
        return Executors.newCachedThreadPool();
    }

}
