package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class SearchForCompletableTest extends CamundaHelpers {

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

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Mock
    private List<TaskOperationService> taskOperationServices;


    @Test
    void should_succeed_and_return_emptyList_when_jurisdiction_is_not_IA() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "invalidJurisdiction",
            "Asylum"
        );

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }

    @Test
    void should_succeed_and_return_emptyList_when_caseType_is_not_Asylum() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "someInvalidCaseType"
        );

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }

    @Test
    void should_succeed_and_return_emptyList_when_no_task_types_returned() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );

        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase)).thenReturn(emptyList());
        when(allowedJurisdictionConfiguration.getAllowedJurisdictions()).thenReturn(List.of("ia"));
        when(allowedJurisdictionConfiguration.getAllowedCaseTypes()).thenReturn(List.of("asylum"));

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }

    @Test
    void should_succeed_and_return_emptyList_when_no_search_results() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );


        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );
        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }

    @Test
    void should_succeed_and_return_emptyList_when_performSearchAction_no_results() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }


    @Test
    void should_succeed_and_return_emptyList_when_performSearchAction_no_results_no_assignee() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );
        when(allowedJurisdictionConfiguration.getAllowedJurisdictions()).thenReturn(List.of("ia"));
        when(allowedJurisdictionConfiguration.getAllowedCaseTypes()).thenReturn(List.of("asylum"));

        when(accessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");
        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());

        CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
        when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
            .thenReturn(camundaSearchQuery);

        List<CamundaTask> searchResults = singletonList(createMockedUnmappedTaskWithNoAssignee());
        when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
            .thenReturn(searchResults);

        when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
            .thenReturn(emptyList());

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
    }

    @Test
    void should_succeed_and_return_tasks_and_is_required_true() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );

        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponse());
        when(accessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

        CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
        when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
            .thenReturn(camundaSearchQuery);

        List<CamundaTask> searchResults = singletonList(createMockedUnmappedTaskWithNoAssignee());
        when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
            .thenReturn(searchResults);

        List<Task> mappedTasksResults = singletonList(createMockedMappedTask());
        when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
            .thenReturn(mappedTasksResults);
        when(allowedJurisdictionConfiguration.getAllowedJurisdictions()).thenReturn(List.of("ia"));
        when(allowedJurisdictionConfiguration.getAllowedCaseTypes()).thenReturn(List.of("asylum"));

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(true, mappedTasksResults), response);
    }

    @Test
    void should_succeed_and_return_tasks_is_required_false() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );

        when(allowedJurisdictionConfiguration.getAllowedJurisdictions()).thenReturn(List.of("ia"));
        when(allowedJurisdictionConfiguration.getAllowedCaseTypes()).thenReturn(List.of("asylum"));

        when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
            .thenReturn(mockTaskCompletionDMNResponseWithEmptyRow());
        when(accessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");
        CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
        when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
            .thenReturn(camundaSearchQuery);

        List<CamundaTask> searchResults = singletonList(createMockedUnmappedTask());
        when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
            .thenReturn(searchResults);

        List<Task> mappedTasksResults = singletonList(createMockedMappedTask());
        when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
            .thenReturn(mappedTasksResults);

        GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse
        );

        assertNotNull(response);
        assertEquals(new GetTasksCompletableResponse<>(false, mappedTasksResults), response);
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

