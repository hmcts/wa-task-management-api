package uk.gov.hmcts.reform.wataskmanagementapi.config.executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@SuppressWarnings("PMD.DoNotUseThreads")
public class ExecutorServiceConfig {

    @Bean("sensitiveTaskEventLogsExecutorService")
    public ExecutorService createSensitiveTaskEventLogsExecutorService() {
        return Executors.newFixedThreadPool(1);
    }

}
