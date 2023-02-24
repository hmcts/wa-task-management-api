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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.TASK_TYPE;

@ExtendWith(MockitoExtension.class)
class TaskSearchControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RoleAssignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private CftQueryService cftQueryService;
    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private TaskSearchController taskSearchController;

    @BeforeEach
    void setUp() {

        taskSearchController = new TaskSearchController(
            accessControlService,
            cftQueryService,
            cftTaskDatabaseService,
            launchDarklyFeatureFlagProvider
        );
    }

    @Test
    void should_succeed_when_performing_search_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(tasksResponse);
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
    void should_succeed_when_performing_search_for_returning_available_tasks_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(tasksResponse);
        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                RequestContext.AVAILABLE_TASKS,
                singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("ia")))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_with_no_pagination_firstResult_and_default_and_return_a_200_ok() {

        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(tasksResponse);
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
        );

        ReflectionTestUtils.setField(taskSearchController, "defaultMaxResults", 50);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, null, 25, searchTaskRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_with_no_pagination_max_results_and_default_and_return_a_200_ok() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(tasksResponse);
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
        );

        ReflectionTestUtils.setField(taskSearchController, "defaultMaxResults", 50);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, null, searchTaskRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
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

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(),
            anyBoolean(), anyBoolean())).thenReturn(tasksResponse);

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

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksCompletableResponse<Task> tasksResponse = new GetTasksCompletableResponse<>(true, taskList);
        when(cftQueryService.searchForCompletableTasks(any(), any(), any(), anyBoolean())).thenReturn(tasksResponse);

        ResponseEntity<GetTasksCompletableResponse<Task>> response =
            taskSearchController.searchWithCriteriaForAutomaticCompletion(
                IDAM_AUTH_TOKEN,
                new SearchEventAndCase("caseId", "eventId", "IA", "caseType")
            );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isTaskRequiredForEvent());
    }

    @Test
    void should_return_200_with_empty_list_for_searchWithCriteria_when_access_control_response_empty() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.empty());

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")))
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalRecords());
    }

    @Test
    void should_return_200_with_empty_list_for_completable_when_access_control_response_empty() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.empty());

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "caseJurisdiction", "caseType");

        ResponseEntity<GetTasksCompletableResponse<Task>> response =
            taskSearchController.searchWithCriteriaForAutomaticCompletion(IDAM_AUTH_TOKEN, searchEventAndCase);


        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isTaskRequiredForEvent());
        assertEquals(0, response.getBody().getTasks().size());
    }

    @Test
    void should_succeed_when_performing_search_by_task_type_id() {
        when(accessControlService.getAccessControlResponse(IDAM_AUTH_TOKEN))
            .thenReturn(Optional.of(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment))));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        GetTasksResponse<Task> tasksResponse = new GetTasksResponse<>(taskList, 1);
        when(cftQueryService.searchForTasks(anyInt(), anyInt(), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(tasksResponse);
        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, 0, 1,
            new SearchTaskRequest(
                singletonList(new SearchParameterList(
                        TASK_TYPE,
                        SearchOperator.IN,
                        singletonList("processApplication")
                    )
                )
            )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

}
