package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/reconfigure_task_data.sql")
public class ExecuteTaskReconfigurationServiceTest {
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    @Autowired
    TaskResourceRepository taskResourceRepository;
    private ExecuteTaskReconfigurationService executeTaskReconfigurationService;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);
        executeTaskReconfigurationService = new ExecuteTaskReconfigurationService(
            cftTaskDatabaseService,
            configureTaskService,
            taskAutoAssignmentService);
    }

    @Test
    void should_get_reconfiguration_fail_log() {
        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        TaskOperationRequest taskOperationRequest = new TaskOperationRequest(
            TaskOperation.builder()
                .name(TaskOperationName.EXECUTE_RECONFIGURE)
                .maxTimeLimit(2)
                .retryWindowHours(1)
                .runId("")
                .build(), taskFilters
        );

        Assertions.assertThatThrownBy(() -> executeTaskReconfigurationService.performOperation(taskOperationRequest))
            .hasMessageContaining("Task Execute Reconfiguration Failed: "
                                  + "Task Reconfiguration process failed to execute "
                                  + "reconfiguration for the following tasks:")
            .hasMessageContaining("8d6cc5cf-c973-11eb-bdba-0242ac222001", "taskName", "ASSIGNED",
                "2022-10-18T10:19:45.345875+01:00", "2022-05-09T20:15:45.345875+01:00", "");
    }

    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.now().minus(Duration.ofDays(10)), TaskFilterOperator.AFTER);
        return List.of(filter);
    }

}
