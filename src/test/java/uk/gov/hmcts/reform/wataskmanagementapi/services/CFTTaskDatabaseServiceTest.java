package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;

@ExtendWith(MockitoExtension.class)
class CFTTaskDatabaseServiceTest {

    @Mock
    TaskResourceRepository taskResourceRepository;
    @Mock
    CFTTaskMapper cftTaskMapper;

    private CFTTaskDatabaseService cftTaskDatabaseService;
    private String taskId;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);

        taskId = UUID.randomUUID().toString();
    }

    @Test
    void should_find_by_id_and_obtain_pessimistic_write_lock() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findById(taskId)).thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_find_by_id_only() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.getByTaskId(taskId)).thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_find_by_case_id() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.getByCaseId("12345")).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService.findByCaseIdOnly("12345");

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_case_ids_states_reconfiguration_time_is_null() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(
            List.of("1234"), List.of(CFTTaskState.ASSIGNED))).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(List.of("1234"), List.of(CFTTaskState.ASSIGNED));

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_not_null() {
        TaskResource someTaskResource = mock(TaskResource.class);
        OffsetDateTime reconfigureRequestTime = OffsetDateTime.now().minusHours(1L);
        when(taskResourceRepository.findByStateInAndReconfigureRequestTimeGreaterThan(
            List.of(CFTTaskState.ASSIGNED), reconfigureRequestTime)).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeGreaterThan(List.of(CFTTaskState.ASSIGNED),
                reconfigureRequestTime);

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_less_than_retry() {
        TaskResource someTaskResource = mock(TaskResource.class);
        OffsetDateTime retry = OffsetDateTime.now().minusHours(2);

        when(taskResourceRepository.findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
            List.of("199"), List.of(CFTTaskState.ASSIGNED), retry)).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
                List.of("199"), List.of(CFTTaskState.ASSIGNED), retry);

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_save_task() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.save(someTaskResource)).thenReturn(someTaskResource);

        final TaskResource actualTaskResource = cftTaskDatabaseService.saveTask(someTaskResource);

        assertNotNull(actualTaskResource);
        verify(someTaskResource, times(1)).getPriorityDate();
        verify(someTaskResource, times(1)).setPriorityDate(any());
        verify(someTaskResource, times(1)).getDueDateTime();
    }

    @Test
    void should_insert_and_lock_task() throws SQLException {

        OffsetDateTime dueDate = OffsetDateTime.now();
        OffsetDateTime created = OffsetDateTime.now().plusMinutes(1);

        lenient().doNothing().when(taskResourceRepository).insertAndLock(taskId, dueDate, created, dueDate);

        cftTaskDatabaseService.insertAndLock(taskId, dueDate);

        verify(taskResourceRepository, times(1))
            .insertAndLock(anyString(),
                any(),
                any(),
                any()
            );
    }

    @Test
    void should_return_task_resource_by_specification() {
        TaskResource taskResource = spy(TaskResource.class);
        Mockito.when(taskResourceRepository.findOne(any())).thenReturn(Optional.of(taskResource));

        final Optional<TaskResource> taskBySpecification = cftTaskDatabaseService.findTaskBySpecification(any());

        assertNotNull(taskBySpecification);
        verify(taskResourceRepository, times(1)).findOne(any());
    }

    @Test
    void should_return_empty_task_resource_by_specification() {
        Mockito.when(taskResourceRepository.findOne(any())).thenReturn(Optional.empty());

        final Optional<TaskResource> taskBySpecification = cftTaskDatabaseService.findTaskBySpecification(any());

        assertTrue(taskBySpecification.isEmpty());
        verify(taskResourceRepository, times(1)).findOne(any());
    }

    @Test
    void should_return_empty_list_when_search_not_find_any_task() {
        when(taskResourceRepository.searchTasksIds(new String[]{"*:IA:*:*:*:765324"}))
            .thenReturn(List.of());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));

        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .build();

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse,
            false);
        assertEquals(0, response.getTotalRecords());
        assertTrue(response.getTasks().isEmpty());
    }

    @Test
    void should_return_task_list_and_count_when_search_find_some_task_and_sort_default_order() {
        List<String> taskIds = List.of("1", "2");
        List<Sort.Order> orders = Stream.of(MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toList());
        TaskResource taskResource = mock(TaskResource.class);
        Task task = mock(Task.class);
        List<TaskResource> taskResources = List.of(taskResource);
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));


        when(taskResourceRepository.searchTasksIds(new String[]{"*:IA:*:*:*:765324"}))
            .thenReturn(taskIds);
        when(taskResourceRepository.findAllByTaskIdIn(taskIds, Sort.by(orders)))
            .thenReturn(taskResources);
        when(taskResourceRepository.searchTasksCount(new String[]{"*:IA:*:*:*:765324"}))
            .thenReturn(1L);
        when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
            eq(taskResource),
            anyList(),
            eq(false)
        )).thenReturn(task);


        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .build();

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse,
            false);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getTasks().size());
        assertEquals(task, response.getTasks().get(0));
    }

    @Test
    void should_return_task_list_and_count_when_search_find_some_task_and_sort_request_order() {
        List<String> taskIds = List.of("1", "2");
        List<Sort.Order> orders = Stream.of(CASE_NAME, MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toList());
        TaskResource taskResource = mock(TaskResource.class);
        Task task = mock(Task.class);
        List<TaskResource> taskResources = List.of(taskResource);
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));


        when(taskResourceRepository.searchTasksIds(new String[]{"*:IA:*:*:*:765324"}))
            .thenReturn(taskIds);
        when(taskResourceRepository.findAllByTaskIdIn(taskIds, Sort.by(orders)))
            .thenReturn(taskResources);
        when(taskResourceRepository.searchTasksCount(new String[]{"*:IA:*:*:*:765324"}))
            .thenReturn(1L);
        when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
            eq(taskResource),
            anyList(),
            eq(false)
        )).thenReturn(task);


        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse,
            false);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getTasks().size());
        assertEquals(task, response.getTasks().get(0));
    }
}
