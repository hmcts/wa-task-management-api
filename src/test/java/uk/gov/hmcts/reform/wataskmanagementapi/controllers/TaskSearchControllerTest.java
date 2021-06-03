package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.google.common.collect.Lists;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.SearchTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;

@ExtendWith(MockitoExtension.class)
class TaskSearchControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private CamundaService camundaService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private Assignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;

    private TaskSearchController taskSearchController;

    @BeforeEach
    void setUp() {

        taskSearchController = new TaskSearchController(
            camundaService,
            accessControlService
        );

    }

    @Test
    void should_succeed_when_performing_search_and_return_a_200_ok() {
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        List<Task> taskList = Lists.newArrayList(mock(Task.class));
        when(camundaService.searchWithCriteria(any(), anyInt(), anyInt(), any(), any())).thenReturn(taskList);
        when(camundaService.getTaskCount(any())).thenReturn(1L);

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, Optional.of(0), Optional.of(1),
            new SearchTaskRequest(
                singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")))
            ));

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalRecords());
    }

    @Test
    void should_succeed_when_performing_search_with_sorting_and_return_a_200_ok() {
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, Optional.of(0), Optional.of(0),
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
            taskSearchController.searchWithCriteria(
                IDAM_AUTH_TOKEN, Optional.of(0), Optional.of(0), new SearchTaskRequest(null)
            );

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_return_a_400_when_performing_search_with_no_parameters() {

        ResponseEntity<GetTasksResponse<Task>> response = taskSearchController.searchWithCriteria(
            IDAM_AUTH_TOKEN, Optional.of(0), Optional.of(0), new SearchTaskRequest(emptyList())
        );

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void should_auto_complete_a_task() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "caseJurisdiction", "caseType");
        ResponseEntity response =
            taskSearchController.searchWithCriteriaForAutomaticCompletion(IDAM_AUTH_TOKEN, searchEventAndCase);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void exception_handler_should_return_200_empty_list_for_no_role_assignments_found_exception() {

        final String exceptionMessage = "Some exception message";
        final NoRoleAssignmentsFoundException exception =
            new NoRoleAssignmentsFoundException(exceptionMessage);

        ResponseEntity<SearchTasksResponse> response = taskSearchController.handleNoRoleAssignmentsException(exception);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(emptyList(), response.getBody().getTasks());

    }
}
