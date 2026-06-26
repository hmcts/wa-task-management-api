package uk.gov.hmcts.reform.wataskmanagementapi.services;

import jakarta.persistence.LockTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithStandardGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithoutAttributes;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_ID;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
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
    void should_find_by_id_and_state_and_obtain_pessimistic_write_lock() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByIdAndStateIn(taskId, List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource =
            cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(taskId, List.of(ASSIGNED, UNASSIGNED));

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_find_by_id_and_wait_and_obtain_pessimistic_write_lock() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByIdAndWaitForLock(taskId)).thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource =
            cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_find_by_id_and_wait_and_obtain_pessimistic_write_lock_throw_exception() {
        when(taskResourceRepository.findByIdAndWaitForLock(taskId)).thenThrow(new LockTimeoutException());

        assertThatThrownBy(() -> cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock(taskId))
            .isInstanceOf(LockTimeoutException.class);
    }

    @Test
    void should_find_by_id_and_state_wait_and_obtain_pessimistic_write_lock_throw_exception() {
        when(taskResourceRepository.findByIdAndStateIn(taskId, List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new LockTimeoutException());

        Exception exception = assertThrowsExactly(LockTimeoutException.class, () -> cftTaskDatabaseService
            .findByIdAndStateInObtainPessimisticWriteLock(taskId, List.of(ASSIGNED, UNASSIGNED)));

        assertNotNull(exception);
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
            List.of("1234"), List.of(ASSIGNED))).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(List.of("1234"), List.of(ASSIGNED));

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_not_null() {
        OffsetDateTime reconfigureRequestTime = OffsetDateTime.now().minusHours(1L);
        doReturn(List.of("1234")).when(taskResourceRepository)
            .findTaskIdsByStateInAndReconfigureRequestTimeGreaterThan(
            List.of(ASSIGNED), reconfigureRequestTime);

        final List<String> actualTaskResource = cftTaskDatabaseService
            .getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
                List.of(ASSIGNED),
                reconfigureRequestTime
            );

        assertNotNull(actualTaskResource);
        assertEquals("1234", actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_less_than_retry() {
        TaskResource someTaskResource = mock(TaskResource.class);
        OffsetDateTime retry = OffsetDateTime.now().minusHours(2);

        when(taskResourceRepository.findByStateInAndReconfigureRequestTimeIsLessThan(
            List.of(ASSIGNED), retry)).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(
                List.of(ASSIGNED), retry);

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
            .insertAndLock(
                anyString(),
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
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .build();

        when(taskResourceRepository.searchTasksIds(eq(1), eq(25),
            anyCollection(),
            eq(List.of()),
            eq(searchRequest)
        ))
            .thenReturn(List.of());
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlResponse.getRoleAssignments())
            .thenReturn(roleAssignmentWithoutAttributes(Classification.PUBLIC));

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(1, 25, searchRequest,
            accessControlResponse
        );
        assertEquals(0, response.getTotalRecords());
        assertTrue(response.getTasks().isEmpty());
    }

    @Test
    void should_build_all_work_role_criteria_with_wildcard_authorization() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.ALL_WORK)
            .build();
        RoleAssignment roleAssignment = roleAssignment(
            "task-supervisor",
            Classification.RESTRICTED,
            Map.of(),
            List.of("skill-1")
        );

        List<TaskSearchRoleCriteria> roleCriteria = capturedRoleCriteriaFor(searchRequest, List.of(roleAssignment));

        assertThat(roleCriteria)
            .containsExactly(new TaskSearchRoleCriteria(
                null,
                null,
                null,
                "task-supervisor",
                null,
                "m",
                "R",
                null
            ));
    }

    @Test
    void should_build_available_task_role_criteria_with_authorizations_and_wildcard() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .build();
        RoleAssignment roleAssignment = roleAssignment(
            "tribunal-caseworker",
            Classification.PRIVATE,
            Map.of(RoleAttributeDefinition.JURISDICTION.value(), "IA"),
            List.of("skill-1", "skill-2", "skill-1")
        );

        List<TaskSearchRoleCriteria> roleCriteria = capturedRoleCriteriaFor(searchRequest, List.of(roleAssignment));

        assertThat(roleCriteria)
            .containsExactly(
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", null),
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", "skill-1"),
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", "skill-2")
            );
    }

    @Test
    void should_build_read_role_criteria_for_case_role_with_wildcard_authorization() {
        SearchRequest searchRequest = SearchRequest.builder().build();
        RoleAssignment roleAssignment = roleAssignment(
            "case-manager",
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.CASE_ID.value(), "case-1"),
            List.of("skill-1")
        );

        List<TaskSearchRoleCriteria> roleCriteria = capturedRoleCriteriaFor(searchRequest, List.of(roleAssignment));

        assertThat(roleCriteria)
            .containsExactly(new TaskSearchRoleCriteria(
                null,
                null,
                null,
                "case-manager",
                "case-1",
                "r",
                "U",
                null
            ));
    }

    @Test
    void should_return_task_list_and_count_when_search_find_some_task_and_sort_default_order() {
        List<String> taskIds = List.of("1");
        List<Sort.Order> orders = Stream.of(MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY, TASK_ID)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toList()); //NOSONAR List needs to be mutable to allow sorting.
        TaskResource taskResource = mock(TaskResource.class);
        Task task = mock(Task.class);
        List<TaskResource> taskResources = List.of(taskResource);
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .build();

        when(accessControlResponse.getRoleAssignments())
            .thenReturn(roleAssignmentWithoutAttributes(Classification.PUBLIC));

        when(taskResourceRepository.searchTasksIds(eq(1), eq(25),
            anyCollection(),
            eq(List.of()),
            eq(searchRequest)
        ))
            .thenReturn(taskIds);
        when(taskResourceRepository.findAllByTaskIdIn(taskIds, Sort.by(orders)))
            .thenReturn(taskResources);
        when(taskResourceRepository.searchTasksCount(
            anyCollection(),
            eq(List.of()),
            eq(searchRequest)
        ))
            .thenReturn(1L);
        when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
            eq(taskResource),
            anyList()
        )).thenReturn(task);

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(1, 25, searchRequest,
            accessControlResponse
        );
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getTasks().size());
        assertEquals(task, response.getTasks().get(0));
    }

    @Test
    void should_return_task_list_and_count_when_search_find_some_task_and_sort_request_order() {
        List<String> taskIds = List.of("1");
        List<Sort.Order> orders = Stream.of(CASE_NAME, MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY, TASK_ID)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toList()); //NOSONAR List needs to be mutable to allow sorting.
        TaskResource taskResource = mock(TaskResource.class);
        Task task = mock(Task.class);
        List<TaskResource> taskResources = List.of(taskResource);
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();

        when(accessControlResponse.getRoleAssignments())
            .thenReturn(roleAssignmentWithoutAttributes(Classification.PUBLIC));

        when(taskResourceRepository.searchTasksIds(eq(1), eq(25),
            anyCollection(),
            eq(List.of()),
            eq(searchRequest)
        ))
            .thenReturn(taskIds);
        when(taskResourceRepository.findAllByTaskIdIn(taskIds, Sort.by(orders)))
            .thenReturn(taskResources);
        when(taskResourceRepository.searchTasksCount(
            anyCollection(),
            eq(List.of()),
            eq(searchRequest)
        ))
            .thenReturn(1L);
        when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
            eq(taskResource),
            anyList()
        )).thenReturn(task);

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(1, 25, searchRequest,
            accessControlResponse
        );
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getTasks().size());
        assertEquals(task, response.getTasks().get(0));
    }

    @Test
    void should_return_task_list_and_count_when_search_find_some_task_other_than_from_excluded_case() {
        List<String> taskIds = List.of("1");
        List<String> caseIds = List.of("1623278362431003");
        List<Sort.Order> orders = Stream.of(MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY, TASK_ID)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toList()); //NOSONAR List needs to be mutable to allow sorting.
        TaskResource taskResource = mock(TaskResource.class);
        Task task = mock(Task.class);
        List<TaskResource> taskResources = List.of(taskResource);
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .build();

        when(accessControlResponse.getRoleAssignments())
            .thenReturn(roleAssignmentWithStandardGrantType(Classification.PUBLIC));

        when(taskResourceRepository.searchTasksIds(eq(1), eq(25),
            anyCollection(),
            eq(caseIds),
            eq(searchRequest)
        ))
            .thenReturn(taskIds);
        when(taskResourceRepository.findAllByTaskIdIn(taskIds, Sort.by(orders)))
            .thenReturn(taskResources);
        when(taskResourceRepository.searchTasksCount(
            anyCollection(),
            eq(caseIds),
            eq(searchRequest)
        ))
            .thenReturn(1L);
        when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
            eq(taskResource),
            anyList()
        )).thenReturn(task);

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(1, 25, searchRequest,
            accessControlResponse
        );
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getTasks().size());
        assertEquals(task, response.getTasks().get(0));
    }

    @Test
    void should_find_task_to_update_index_return_list_of_tasks() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByIndexedFalseAndStateIn(List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService.findTaskToUpdateIndex();

        assertNotNull(actualTaskResource);
        assertEquals(1, actualTaskResource.size());
    }

    @Test
    void should_mark_tasks_for_deletion_by_task_ids() {
        final List<String> taskIds = List.of("123", "456");
        doNothing().when(taskResourceRepository).updateTaskDeletionTimestampByTaskIds(taskIds);

        cftTaskDatabaseService.markTasksToDeleteByTaskId(taskIds);

        verify(taskResourceRepository, times(1))
            .updateTaskDeletionTimestampByTaskIds(taskIds);
    }


    @Test
    void should_return_successfully_when_user_has_large_number_of_role_assignments() {
        final int logThreshold = 100;
        final SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .locations(List.of("12345"))
            .build();

        List<RoleAssignment> roleAssignments = spy(new ArrayList<>());
        for (int index = 0; index < logThreshold; index++) {
            roleAssignments.add(roleAssignmentWithoutAttributes(Classification.PUBLIC).get(0));
        }

        when(taskResourceRepository.searchTasksIds(
            eq(1),
            eq(25),
            anyCollection(),
            eq(List.of()),
            eq(searchRequest))
        ).thenReturn(List.of());

        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(
            1,
            25,
            searchRequest,
            accessControlResponse
        );

        assertEquals(0, response.getTotalRecords());
        assertTrue(response.getTasks().isEmpty());

        verify(roleAssignments, times(2)).size();
    }

    private List<TaskSearchRoleCriteria> capturedRoleCriteriaFor(SearchRequest searchRequest,
                                                                 List<RoleAssignment> roleAssignments) {
        when(taskResourceRepository.searchTasksIds(
            eq(0),
            eq(25),
            anyCollection(),
            eq(List.of()),
            eq(searchRequest))
        ).thenReturn(List.of());

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);

        cftTaskDatabaseService.searchForTasks(0, 25, searchRequest, accessControlResponse);

        ArgumentCaptor<Collection<TaskSearchRoleCriteria>> roleCriteriaCaptor = ArgumentCaptor.forClass(
            Collection.class
        );
        verify(taskResourceRepository).searchTasksIds(
            eq(0),
            eq(25),
            roleCriteriaCaptor.capture(),
            eq(List.of()),
            eq(searchRequest)
        );

        return List.copyOf(roleCriteriaCaptor.getValue());
    }

    private RoleAssignment roleAssignment(String roleName,
                                          Classification classification,
                                          Map<String, String> attributes,
                                          List<String> authorisations) {
        return RoleAssignment.builder()
            .roleName(roleName)
            .classification(classification)
            .grantType(GrantType.STANDARD)
            .attributes(attributes)
            .authorisations(authorisations)
            .build();
    }
}
