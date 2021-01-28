package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private CamundaService camundaService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private Assignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;

    private TaskController taskController;

    @BeforeEach
    void setUp() {

        taskController = new TaskController(
            camundaService,
            accessControlService
        );

    }

    @Test
    void should_succeed_when_fetching_a_task_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();

        Task mockedTask = mock(Task.class);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        when(camundaService.getTask(taskId, singletonList(mockedRoleAssignment), singletonList(PermissionTypes.READ)))
            .thenReturn(mockedTask);

        ResponseEntity<GetTaskResponse<Task>> response = taskController.getTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetTaskResponse.class));
        assertNotNull(response.getBody());
        assertEquals(mockedTask, response.getBody().getTask());
    }

    @Test
    void should_succeed_when_claiming_a_task_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();

        ResponseEntity<String> response = taskController.claimTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_when_performing_search_and_return_a_200_ok() {
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        ResponseEntity<GetTasksResponse<Task>> response = taskController.searchWithCriteria(
            IDAM_AUTH_TOKEN,
            new SearchTaskRequest(
                singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")))
            ));

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_succeed_when_performing_search_with_sorting_and_return_a_200_ok() {
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        ResponseEntity<GetTasksResponse<Task>> response = taskController.searchWithCriteria(
            IDAM_AUTH_TOKEN,
            new SearchTaskRequest(
                singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
                singletonList(new SortingParameter(SortField.DUE_DATE, SortOrder.DESCENDANT)))
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_return_a_400_when_performing_search_with_null_parameters() {

        ResponseEntity<GetTasksResponse<Task>> response =
            taskController.searchWithCriteria(IDAM_AUTH_TOKEN, new SearchTaskRequest(null));

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_return_a_400_when_performing_search_with_no_parameters() {

        ResponseEntity<GetTasksResponse<Task>> response =
            taskController.searchWithCriteria(IDAM_AUTH_TOKEN, new SearchTaskRequest(emptyList()));

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_unclaim_a_task_204_no_content() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";

        ResponseEntity<String> response = taskController.unclaimTask(authToken, taskId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_assigning_task() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";

        ResponseEntity<Void> response = taskController.assignTask(
            authToken,
            taskId,
            new AssigneeRequest("userId")
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_complete_a_task() {
        String taskId = UUID.randomUUID().toString();
        ResponseEntity response = taskController.completeTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_auto_complete_a_task() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "caseJurisdiction", "caseType");
        ResponseEntity response =
            taskController.searchWithCriteriaForAutomaticCompletion(IDAM_AUTH_TOKEN, searchEventAndCase);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_cancel_a_task() {
        String taskId = UUID.randomUUID().toString();
        ResponseEntity response = taskController.cancelTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
