package uk.gov.hmcts.reform.wataskmanagementapi.config.executors;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@SuppressWarnings("PMD.DoNotUseThreads")
public class SensitiveTaskEventLogsExecutor {

    @Autowired
    public ExecutorService sensitiveTaskEventLogsExecutorService;


    @PreDestroy
    public void cleanup() {
        log.info("Shutting down SensitiveTaskEventLog executor");
        sensitiveTaskEventLogsExecutorService.shutdown();
        try {
            // Wait a while for sensitiveTaskEventLog to be saved
            if (!sensitiveTaskEventLogsExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                sensitiveTaskEventLogsExecutorService.shutdownNow(); // Cancel currently execution
                // Wait a while for tasks to respond to being cancelled
                if (!sensitiveTaskEventLogsExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            sensitiveTaskEventLogsExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down SensitiveTaskEventLog executor");
    }
}
