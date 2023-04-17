package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ExecuteTaskReconfigurationFailureService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/reconfigure_task_data.sql")
public class ExecuteTaskReconfigurationFailureServiceTest {
    @Autowired
    TaskResourceRepository taskResourceRepository;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    private ExecuteTaskReconfigurationFailureService executeTaskReconfigurationFailureService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        CFTTaskDatabaseService cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository,
            cftTaskMapper);
        executeTaskReconfigurationFailureService = new ExecuteTaskReconfigurationFailureService(
            cftTaskDatabaseService);
    }

    @Test
    void should_get_reconfiguration_fail_log() {
        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        TaskOperationRequest taskOperationRequest = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES)
                .maxTimeLimit(2)
                .retryWindowHours(1)
                .runId("")
                .build(), taskFilters
        );

        List<TaskResource> failedTasks = executeTaskReconfigurationFailureService.performOperation(
            taskOperationRequest
        );

        assertEquals(failedTasks.size(), 1);
    }

    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.parse("2022-10-17T10:19:45.345875+01:00"),
            TaskFilterOperator.AFTER);
        return List.of(filter);
    }

}
