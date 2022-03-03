package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_TASK_QUERY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;

@ExtendWith(MockitoExtension.class)
class TaskSearchControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private TaskManagementService taskManagementService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RoleAssignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private CftQueryService cftQueryService;
    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private TaskSearchController taskSearchController;

    @BeforeEach
    void setUp() {

        taskSearchController = new TaskSearchController(
            taskManagementService,
            accessControlService,
            cftQueryService,
            launchDarklyFeatureFlagProvider
        );
    }

    @Test
    void should_succeed_when_performing_search_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any())).thenReturn(taskList);
        when(taskManagementService.getTaskCount(any())).thenReturn(1L);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_for_returning_available_tasks_only_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any())).thenReturn(taskList);
        when(taskManagementService.getTaskCount(any())).thenReturn(1L);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_fail_when_performing_search_for_returning_available_tasks_only_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any())).thenReturn(taskList);
        when(taskManagementService.getTaskCount(any())).thenReturn(1L);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_with_no_pagination_firstResult_and_default_and_return_a_200_ok() {

        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any())).thenReturn(taskList);
        when(taskManagementService.getTaskCount(any())).thenReturn(1L);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
        );

        ReflectionTestUtils.setField(taskSearchController, "defaultMaxResults", 50);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, null, 25, searchTaskRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
        verify(taskManagementService, times(1))
            .searchWithCriteria(
                eq(searchTaskRequest),
                eq(0),
                eq(25),
                any(AccessControlResponse.class)
            );
    }

    @Test
    void should_succeed_when_performing_search_with_no_pagination_max_results_and_default_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(taskManagementService.searchWithCriteria(any(), anyInt(), anyInt(), any())).thenReturn(taskList);
        when(taskManagementService.getTaskCount(any())).thenReturn(1L);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
        );

        ReflectionTestUtils.setField(taskSearchController, "defaultMaxResults", 50);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, null, searchTaskRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
        verify(taskManagementService, times(1))
            .searchWithCriteria(
                eq(searchTaskRequest),
                eq(0),
                eq(50),
                any(AccessControlResponse.class)
            );
    }

    @Test
    void should_succeed_when_performing_search_with_sorting_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 0,
            new SearchTaskRequest(
                singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
                singletonList(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_return_a_400_when_performing_search_with_null_parameters() {

        ResponseEntity<GetTasksResponse<Task>> response =
            taskSearchController.searchWithCriteria(
                IDAM_AUTH_TOKEN, 0, 0, new SearchTaskRequest(null)
            );

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_return_a_400_when_performing_search_with_no_parameters() {

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 0, new SearchTaskRequest(emptyList())
        );

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_auto_complete_a_task() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "caseJurisdiction", "caseType");
        ResponseEntity<GetTasksCompletableResponse<Task>> response =
            taskSearchController.searchWithCriteriaForAutomaticCompletion(IDAM_AUTH_TOKEN, searchEventAndCase);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_succeed_when_performing_search_with_feature_flag_on_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                mockedUserInfo.getUid(),
                mockedUserInfo.getEmail()
            )
        ).thenReturn(true);

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), any())).thenReturn(tasksResponse);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_for_completable_with_feature_flag_on_and_return_a_200_ok() {

        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                mockedUserInfo.getUid(),
                mockedUserInfo.getEmail()
            )
        ).thenReturn(true);

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksCompletableResponse<Task> tasksResponse = new GetTasksCompletableResponse<>(true, taskList);
        when(cftQueryService.searchForCompletableTasks(any(), any(), any())).thenReturn(tasksResponse);

        ResponseEntity<GetTasksCompletableResponse<Task>> response =
            taskSearchController.searchWithCriteriaForAutomaticCompletion(
                IDAM_AUTH_TOKEN,
                new SearchEventAndCase("caseId", "eventId", "IA", "caseType")
            );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isTaskRequiredForEvent());
    }
}
