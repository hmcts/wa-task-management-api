package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkTaskReconfigurationServiceTest {

    private static final String IDAM_SYSTEM_USER = "IDAM_SYSTEM_USER";

    @Mock
    CaseConfigurationProviderService caseConfigurationProviderService;
    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private IdamTokenGenerator idamTokenGenerator;
    @Mock
    private UserInfo userInfo;
    @InjectMocks
    private MarkTaskReconfigurationService markTaskReconfigurationService;

    @BeforeEach
    void setup() {
        lenient().when(caseConfigurationProviderService.evaluateConfigurationDmn(
            anyString(),
            anyMap()
        )).thenReturn(List.of(
            new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue("caseName"),
                CamundaValue.stringValue("Value"),
                CamundaValue.booleanValue(true)
            )
        ));
        lenient().when(idamTokenGenerator.generate()).thenReturn("token");
        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn(IDAM_SYSTEM_USER);
    }


    @Test
    void should_mark_tasks_to_reconfigure_if_task_resource_is_not_already_marked() {

        List<TaskFilter<?>> taskFilters = createTaskFilters();
        List<TaskResource> taskResources = taskResources(null);
        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(anyString(), anyList()))
            .thenReturn(Optional.of(taskResources.get(0)))
            .thenReturn(Optional.of(taskResources.get(1)));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));

        TaskOperationResponse taskOperationResponse = markTaskReconfigurationService
            .markTasksToReconfigure(taskFilters);

        verify(caseConfigurationProviderService, times(0)).evaluateConfigurationDmn(anyString(),
                                                                                    any());

        int taskResourcesMarked = (int) taskOperationResponse.getResponseMap()
            .get("successfulTaskResources");

        assertEquals(2, taskResourcesMarked);

    }

    @Test
    void should_not_mark_tasks_to_reconfigure_if_task_resource_is_not_active() {
        List<TaskFilter<?>> taskFilters = createTaskFilters();

        List<TaskResource> taskResources = cancelledTaskResources();
        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(taskResources);

        TaskOperationResponse taskOperationResponse = markTaskReconfigurationService
            .markTasksToReconfigure(taskFilters);

        int taskResourcesMarked = (int) taskOperationResponse.getResponseMap()
            .get("successfulTaskResources");

        assertEquals(0, taskResourcesMarked);

    }

    @Test
    void should_not_mark_tasks_to_reconfigure_if_task_state_is_changed_to_not_active() {
        List<TaskResource> taskResources = taskResources(null);
        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(taskResources);
        taskResources.get(0).setState(CFTTaskState.CANCELLED);
        cftTaskDatabaseService.saveTask(taskResources.get(0));
        List<TaskFilter<?>> taskFilters = createTaskFilters();

        TaskOperationResponse taskOperationResponse = markTaskReconfigurationService
            .markTasksToReconfigure(taskFilters);

        int taskResourcesMarked = (int) taskOperationResponse.getResponseMap()
            .get("successfulTaskResources");

        assertEquals(0, taskResourcesMarked);

    }

    @Test
    void should_not_mark_tasks_to_reconfigure_if_task_resource_is_already_marked_to_configure() {
        List<TaskFilter<?>> taskFilters = createTaskFilters();

        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(List.of());

        TaskOperationResponse taskOperationResponse = markTaskReconfigurationService
            .markTasksToReconfigure(taskFilters);

        int taskResourcesMarked = (int) taskOperationResponse.getResponseMap()
            .get("successfulTaskResources");

        assertEquals(0, taskResourcesMarked);
    }

    private List<TaskFilter<?>> createTaskFilters() {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private List<TaskResource> taskResources(OffsetDateTime reconfigureTime) {
        TaskResource taskResource1 = new TaskResource(
            "1234",
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNASSIGNED,
            "someCaseId"
        );
        TaskResource taskResource2 = new TaskResource(
            "4567",
            "someTaskName",
            "someTaskType",
            CFTTaskState.ASSIGNED,
            "someCaseId"
        );
        if (Objects.nonNull(reconfigureTime)) {
            taskResource1.setReconfigureRequestTime(reconfigureTime);
            taskResource2.setReconfigureRequestTime(reconfigureTime);
        }
        return List.of(taskResource1, taskResource2);
    }

    private List<TaskResource> cancelledTaskResources() {
        TaskResource taskResource1 = new TaskResource(
            "5678",
            "someTaskName",
            "someTaskType",
            CFTTaskState.CANCELLED,
            "someCaseId"
        );
        TaskResource taskResource2 = new TaskResource(
            "6789",
            "someTaskName",
            "someTaskType",
            CFTTaskState.CANCELLED,
            "someCaseId"
        );
        return List.of(taskResource1, taskResource2);
    }
}
