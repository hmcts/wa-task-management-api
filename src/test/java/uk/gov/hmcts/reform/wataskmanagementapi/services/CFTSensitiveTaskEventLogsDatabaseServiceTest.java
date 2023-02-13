package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CFTSensitiveTaskEventLogsDatabaseServiceTest {

    @Mock
    private SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @BeforeEach
    void setUp() {
        cftSensitiveTaskEventLogsDatabaseService =
            new CFTSensitiveTaskEventLogsDatabaseService(sensitiveTaskEventLogsRepository, cftTaskDatabaseService);
    }

    @Test
    void should_save_sensitive_task_event_log() {
        SensitiveTaskEventLog sensitiveTaskEventLog = mock(SensitiveTaskEventLog.class);

        when(cftSensitiveTaskEventLogsDatabaseService.saveSensitiveTaskEventLog(sensitiveTaskEventLog))
            .thenReturn(sensitiveTaskEventLog);

        final SensitiveTaskEventLog actualSensitiveTaskEventLog =
            cftSensitiveTaskEventLogsDatabaseService.saveSensitiveTaskEventLog(sensitiveTaskEventLog);

        assertNotNull(actualSensitiveTaskEventLog);
    }
}
