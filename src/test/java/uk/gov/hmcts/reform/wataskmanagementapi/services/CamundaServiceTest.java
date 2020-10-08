package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CamundaServiceTest {

    @Mock
    private CamundaServiceApi camundaServiceApi;

    @Mock
    private CamundaErrorDecoder camundaErrorDecoder;

    @Mock
    private CamundaQueryBuilder camundaQueryBuilder;

    @Mock
    private TaskMapper taskMapper;

    private CamundaService camundaService;

    @BeforeEach
    public void setUp() {
        camundaService = new CamundaService(
            camundaServiceApi,
            camundaQueryBuilder,
            taskMapper,
            camundaErrorDecoder
        );
    }

    @Test
    void getTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();

        CamundaTask mockedTask = mock(CamundaTask.class);
        when(camundaServiceApi.getTask(taskId)).thenReturn(mockedTask);

        CamundaTask response = camundaService.getTask(taskId);

        verify(camundaServiceApi, times(1)).getTask(taskId);
        verifyNoMoreInteractions(camundaServiceApi);

        assertEquals(mockedTask, response);
    }


    @Test
    void getTask_should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();

        when(camundaServiceApi.getTask(taskId)).thenThrow(FeignException.class);

        assertThatThrownBy(() -> camundaService.getTask(taskId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

    @Test
    void claimTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        camundaService.claimTask(taskId, userId);
        verify(camundaServiceApi, times(1)).claimTask(eq(taskId), anyMap());
        verifyNoMoreInteractions(camundaServiceApi);
    }

    @Test
    void claimTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase()
            );

        doThrow(exception)
            .when(camundaServiceApi).claimTask(eq(taskId), anyMap());

        assertThatThrownBy(() -> camundaService.claimTask(taskId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

    @Test
    void claimTask_should_throw_server_error_exception_when_other__exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String exceptionMessage = "some exception message";

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                exceptionMessage
            );

        doThrow(exception)
            .when(camundaServiceApi).claimTask(eq(taskId), anyMap());

        when(camundaErrorDecoder.decode(anyString())).thenReturn(exceptionMessage);

        assertThatThrownBy(() -> camundaService.claimTask(taskId, userId))
            .isInstanceOf(ServerErrorException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage(String.format(
                "Could not claim the task with id: %s. %s", taskId, exceptionMessage
            ));
    }

    @Test
    void searchWithCriteria_should_succeed() throws JsonProcessingException {


        SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
        CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null
        );

        Map<String, CamundaVariable> variables = mockVariables();

        when(camundaQueryBuilder.createQuery(searchTaskRequest))
            .thenReturn(camundaSearchQueryMock);
        when(camundaServiceApi.searchWithCriteria(camundaSearchQueryMock.getQueries()))
            .thenReturn(singletonList(camundaTask));
        when(camundaServiceApi.getVariables(camundaTask.getId()))
            .thenReturn(variables);
        when(taskMapper.mapToTaskObject(camundaTask, variables))
            .thenCallRealMethod();

        List<Task> results = camundaService.searchWithCriteria(searchTaskRequest);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("configured", results.get(0).getState());
        assertEquals(dueDate, results.get(0).getDueDate());
        assertEquals("someTaskName", results.get(0).getName());
        assertNotNull(results.get(0).getCaseData());
        assertEquals("someCaseType", results.get(0).getCaseData().getCategory());
        assertEquals("someCaseName", results.get(0).getCaseData().getName());
        assertNotNull(results.get(0).getCaseData().getLocation());
        assertEquals("someStaffLocationId", results.get(0).getCaseData().getLocation().getId());
        assertEquals("someStaffLocationName", results.get(0).getCaseData().getLocation().getLocationName());
        assertNotNull(results.get(0).getAssignee());
        assertEquals("someAssignee", results.get(0).getAssignee().getId());
        assertEquals("username", results.get(0).getAssignee().getUserName());
        verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
        verifyNoMoreInteractions(camundaQueryBuilder);
        verify(camundaServiceApi, times(1)).searchWithCriteria(camundaSearchQueryMock.getQueries());
        verify(camundaServiceApi, times(1)).getVariables(camundaTask.getId());
        verifyNoMoreInteractions(camundaServiceApi);
    }


    @Test
    void getTask_should_throw_a_server_error_exception_when_camunda_search_call_fails() {

        SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
        CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);


        when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);

        when(camundaServiceApi.searchWithCriteria(any())).thenThrow(FeignException.class);

        assertThatThrownBy(() -> camundaService.searchWithCriteria(searchTaskRequest))
            .isInstanceOf(ServerErrorException.class)
            .hasMessage("There was a problem performing the search")
            .hasCauseInstanceOf(FeignException.class);
    }

    @Test
    void getTask_should_throw_a_server_error_exception_when_camunda_local_variables_call_fails() {


        SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
        CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);


        when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);

        when(camundaServiceApi.searchWithCriteria(camundaSearchQueryMock.getQueries())).thenReturn(singletonList(mock(
            CamundaTask.class)));
        when(camundaServiceApi.getVariables(any())).thenThrow(FeignException.class);


        assertThatThrownBy(() -> camundaService.searchWithCriteria(searchTaskRequest))
            .isInstanceOf(ServerErrorException.class)
            .hasMessage("There was a problem performing the search")
            .hasCauseInstanceOf(FeignException.class);

    }


    @Test
    void unclaimTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();

        camundaService.unclaimTask(taskId);
        verify(camundaServiceApi, times(1)).unclaimTask(eq(taskId));
    }

    @Test
    void unclaimTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();
        String exceptionMessage = "some exception message";

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                exceptionMessage
            );

        doThrow(exception)
            .when(camundaServiceApi).unclaimTask(eq(taskId));

        assertThatThrownBy(() -> camundaService.unclaimTask(taskId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);
    }

    @Test
    void should_throw_an_exception_when_completing_task_and_feign_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();

        doThrow(mock(FeignException.class)).when(camundaServiceApi).completeTask(taskId, new CompleteTaskVariables());

        assertThatThrownBy(() -> camundaService.completeTask(taskId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

    @Test
    void should_complete_task() {
        String taskId = UUID.randomUUID().toString();
        camundaService.completeTask(taskId);

        Map<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("taskState", CamundaValue.stringValue("completed"));
        Mockito.verify(camundaServiceApi).addLocalVariablesToTask(taskId, new AddLocalVariableRequest(modifications));
        Mockito.verify(camundaServiceApi).completeTask(taskId, new CompleteTaskVariables());
    }

    @Test
    void does_not_call_camunda_complete_if_task_already_complete() {
        String taskId = UUID.randomUUID().toString();
        when(camundaServiceApi.getTaskVariables(taskId)).thenReturn(singletonList(
            new HistoryVariableInstance("taskState", "completed")
        ));

        camundaService.completeTask(taskId);

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("taskState", CamundaValue.stringValue("completed"));
        Mockito.verifyNoMoreInteractions(camundaServiceApi);
    }

    @Test
    void assigneeTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        camundaService.assigneeTask(taskId, userId);
        verify(camundaServiceApi, times(1)).assigneeTask(eq(taskId), anyMap());
        verify(camundaServiceApi, times(1)).addLocalVariablesToTask(eq(taskId), any());
        verifyNoMoreInteractions(camundaServiceApi);
    }

    @Test
    void assigneeTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase()
            );

        doThrow(exception)
            .when(camundaServiceApi).assigneeTask(eq(taskId), anyMap());

        assertThatThrownBy(() -> camundaService.assigneeTask(taskId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

    private Map<String, CamundaVariable> mockVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("staffLocationId", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("staffLocation", new CamundaVariable("someStaffLocationName", "String"));

        return variables;
    }


}
