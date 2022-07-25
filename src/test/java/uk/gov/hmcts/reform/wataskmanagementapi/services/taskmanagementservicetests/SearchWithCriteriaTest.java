package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@ExtendWith(MockitoExtension.class)
class SearchWithCriteriaTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    @Mock
    CamundaService camundaService;
    @Mock
    CamundaQueryBuilder camundaQueryBuilder;
    @Mock
    PermissionEvaluatorService permissionEvaluatorService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    private List<TaskOperationService> taskOperationServices;

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;



    @Test
    void searchWithCriteria_should_succeed_and_return_emptyList() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
        when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(null);

        List<Task> response = taskManagementService.searchWithCriteria(
            searchTaskRequest,
            0,
            1,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(emptyList(), response);
    }

    @Test
    void searchWithCriteria_should_succeed_and_return_mapped_tasks() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
        CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
        Task mockedMappedTask = createMockedMappedTask();
        when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQuery);
        when(camundaService.searchWithCriteria(
            camundaSearchQuery,
            0,
            1,
            accessControlResponse,
            singletonList(READ)
        )).thenReturn(singletonList(mockedMappedTask));

        List<Task> response = taskManagementService.searchWithCriteria(
            searchTaskRequest,
            0,
            1,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(mockedMappedTask, response.get(0));
    }

    @BeforeEach
    public void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftQueryService
        );
        taskManagementService = new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            taskOperationServices,
            entityManager,
            allowedJurisdictionConfiguration
        );


        taskId = UUID.randomUUID().toString();
    }
}

