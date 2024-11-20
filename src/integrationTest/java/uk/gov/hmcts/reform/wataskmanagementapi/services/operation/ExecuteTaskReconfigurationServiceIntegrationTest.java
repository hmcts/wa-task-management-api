package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskExecuteReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.OptimisticLockException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;

@ActiveProfiles("integration")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/execute_reconfigure_task_data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class ExecuteTaskReconfigurationServiceIntegrationTest {
    @Autowired
    TaskResourceRepository taskResourceRepository;
    @MockBean
    TaskAutoAssignmentService taskAutoAssignmentService;
    ExecuteTaskReconfigurationService executeTaskReconfigurationService;

    @SpyBean
    TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler;
    @SpyBean
    CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    CaseConfigurationProviderService caseConfigurationProviderService;

    List<TaskResource> taskResources;

    @BeforeEach
    void setUp() {
        executeTaskReconfigurationService = new ExecuteTaskReconfigurationService(
            cftTaskDatabaseService,
            taskReconfigurationTransactionHandler
        );

        taskResources = taskResourceRepository.findAllByTaskIdIn(
            List.of("8d6cc5cf-c973-11eb-bdba-0242ac222009",
                  "8d6cc5cf-c973-11eb-bdba-0242ac222019",
                  "8d6cc5cf-c973-11eb-bdba-0242ac222029"), Sort.by(Sort.Order.asc("taskId"))
        );
    }

    /*
   Scenario: Task reconfiguration with exception handling
   This test verifies that task reconfiguration is successful for the first and third tasks.
   It also ensures that if an exception occurs during the reconfiguration of the second task,
   the reconfiguration is retried, and if it fails again, the transaction for that specific task is rolled back.
   */
    @Test
    void should_retry_task_reconfiguration_for_that_task_when_any_exception_occurs() {
        TaskResource taskResource1 = taskResources.get(0);
        assert taskResource1 != null;
        taskResource1.setTitle("title1");
        doReturn(taskResource1).when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource1);

        TaskResource taskResource2 = taskResources.get(1);
        assert taskResource2 != null;
        taskResource2.setTitle("title1");
        doThrow(new OptimisticLockException("locked"))
            .when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource2);

        TaskResource taskResource3 = taskResources.get(2);
        assert taskResource3 != null;
        taskResource3.setTitle("title1");
        doReturn(taskResource3).when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource3);

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(),
            emptyList()
        );
        doReturn(results).when(caseConfigurationProviderService)
            .getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean());
        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .runId("")
                .build(),
            taskFilters
        );

        assertThrows(TaskExecuteReconfigurationException.class,
                     () -> executeTaskReconfigurationService.performOperation(request));

        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource1);
        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource2);
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource3);

        verify(taskReconfigurationTransactionHandler, times(1))
            .reconfigureTaskResource(taskResource1.getTaskId());
        // taskResource2 will be retried 4 times
        verify(taskReconfigurationTransactionHandler, times(4))
            .reconfigureTaskResource(taskResource2.getTaskId());
        verify(taskReconfigurationTransactionHandler, times(1))
            .reconfigureTaskResource(taskResource3.getTaskId());

        final TaskResource taskResource1AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource1.getTaskId()).orElse(null);
        final TaskResource taskResource2AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource2.getTaskId()).orElse(null);
        final TaskResource taskResource3AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource3.getTaskId()).orElse(null);

        assertAll(() -> {
                assertNotNull(taskResource1AfterReconfigure);
                assertEquals("title1", taskResource1AfterReconfigure.getTitle());
                assertNotNull(taskResource2AfterReconfigure);
                assertEquals("title", taskResource2AfterReconfigure.getTitle());
                assertNotNull(taskResource3AfterReconfigure);
                assertEquals("title1", taskResource3AfterReconfigure.getTitle());
            }
        );

    }



    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.now().minus(Duration.ofDays(1)),
            TaskFilterOperator.AFTER
        );
        return List.of(filter);
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse() {
        return List.of(
                new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                        booleanValue(true)
                )
        );
    }
}

