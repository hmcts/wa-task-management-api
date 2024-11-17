package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.TransactionStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskExecuteReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;

import javax.persistence.OptimisticLockException;
import javax.xml.transform.dom.DOMResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;

@ActiveProfiles("integration")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/execute_reconfigure_task_data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class ExecuteTaskReconfigurationServiceTest {
    @Autowired
    TaskResourceRepository taskResourceRepository;
    @SpyBean
    TaskAutoAssignmentService taskAutoAssignmentService;
    ExecuteTaskReconfigurationService executeTaskReconfigurationService;

    @SpyBean
    TaskReconfigurationHelper taskReconfigurationHelper;
    @SpyBean
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    TransactionTemplate transactionTemplate;

    @SpyBean
    CaseConfigurationProviderService caseConfigurationProviderService;

    List<TaskResource> taskResources;

    @BeforeEach
    void setUp() {
        executeTaskReconfigurationService = new ExecuteTaskReconfigurationService(
            cftTaskDatabaseService,
            taskReconfigurationHelper
        );

        taskResources = taskResourceRepository.findAllByTaskIdIn(
            List.of("8d6cc5cf-c973-11eb-bdba-0242ac222009",
                  "8d6cc5cf-c973-11eb-bdba-0242ac222019",
                  "8d6cc5cf-c973-11eb-bdba-0242ac222029"), Sort.by(Sort.Order.asc("taskId"))
        );
    }

    @Test
    void should_rollback_changes_when_exception_during_reAutoAssignCFTTask(){
        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        TaskResource taskResource1 = taskResources.get(0);
        TaskResource taskResource2 = taskResources.get(1);
        TaskResource taskResource3 = taskResources.get(2);
        assert taskResource1 != null;
        taskResource1.setTitle("title1");
        doReturn(taskResource1).when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource1);

        assert taskResource2 != null;
        taskResource2.setTitle("title1");
        doThrow(new RuntimeException("Error")).when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource2);

        assert taskResource3 != null;
        taskResource3.setTitle("title1");
        doReturn(taskResource3).when(taskAutoAssignmentService).reAutoAssignCFTTask(taskResource3);

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(),
            permissionsResponse()
        );
        doReturn(results).when(caseConfigurationProviderService).getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean());

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .runId("")
                .build(),
            taskFilters
        );

        doInTransaction(()->executeTaskReconfigurationService.performOperation(request));
        verify(cftTaskDatabaseService, times(2)).saveTask(any(TaskResource.class));

        final TaskResource taskResource1AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource1.getTaskId()).orElse(null);
        final TaskResource taskResource2AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource2.getTaskId()).orElse(null);
        final TaskResource taskResource3AfterReconfigure =
            taskResourceRepository.getByTaskId(taskResource3.getTaskId()).orElse(null);


        assertAll(
            () -> {
                assert taskResource1AfterReconfigure != null;
                assertEquals("title1", taskResource1AfterReconfigure.getTitle());
            },
            () -> {
                assert taskResource2AfterReconfigure != null;
                assertEquals("title", taskResource2AfterReconfigure.getTitle());
            },
            () -> {
                assert taskResource3AfterReconfigure != null;
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

    private List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            )
        );
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse() {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                                                   booleanValue(true)
            )
        );
    }

    private void doInTransaction(Runnable runnable) {
        transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRED);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                runnable.run();
                }  catch (TaskExecuteReconfigurationException ex) {
                    log.error("TaskExecuteReconfigurationException Exception {}", ex.getMessage());// Log the exception or take specific actions, but do not mark for rollback
                } catch(Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            }
        });
    }

}

