package uk.gov.hmcts.reform.wataskmanagementapi.config.executors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SensitiveTaskEventLogsExecutorTest {

    @Autowired
    private ExecutorService sensitiveTaskEventLogsExecutorService;


    @Test
    @DisplayName("Test if the executor is shutdown properly")
    void testExecutorShutdown() {
        SensitiveTaskEventLogsExecutor executor = new SensitiveTaskEventLogsExecutor();
        executor.sensitiveTaskEventLogsExecutorService = Executors.newSingleThreadExecutor();

        // Submit a task to the executor
        executor.sensitiveTaskEventLogsExecutorService.submit(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                fail("Interrupted exception occurred during test");
            }
        });

        // Shutdown the executor
        executor.cleanup();

        // Verify that the executor has been shutdown properly
        assertTrue(executor.sensitiveTaskEventLogsExecutorService.isTerminated(), "Executor should have been shutdown");
    }
}
