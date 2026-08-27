package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TransactionHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AwaitilityIntegrationTestConfig;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Import(AwaitilityIntegrationTestConfig.class)
@Slf4j
class TaskResourceRepositoryTest {

    public static final Map<String, String> ADDITIONAL_PROPERTIES = Map.of(
        "name1", "value1",
        "name2", "value2",
        "name3", "value3"
    );

    private String taskId;
    private TaskResource task;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    protected TransactionHelper transactionHelper;

    @Autowired
    private TaskRoleResourceRepository taskRoleResourceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @AfterEach
    void tearDown() {
        taskRoleResourceRepository.deleteAll();
        taskResourceRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        task = createTask(taskId, "tribunal-caseofficer", "IA",
            "startAppeal", "someAssignee", "1623278362430412", CFTTaskState.ASSIGNED);
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(task));
    }

    @Test
    @Timeout(value = 15, unit = SECONDS)
    @Execution(ExecutionMode.CONCURRENT)
    void should_insert_and_lock_when_concurrent_calls_for_different_task_id_then_succeed()
        throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        OffsetDateTime created = OffsetDateTime.parse("2022-05-08T20:15:45.345875+01:00");
        OffsetDateTime dueDate = OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00");
        OffsetDateTime priorityDate = OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00");

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "some task name",
            "some task type",
            CFTTaskState.ASSIGNED,
            created,
            dueDate,
            priorityDate
        );
        taskResource.setCreated(created);

        executorService.execute(() -> {
            taskResourceRepository.insertAndLock(
                taskResource.getTaskId(),
                taskResource.getCreated(),
                taskResource.getDueDateTime(),
                taskResource.getPriorityDate()
            );
            await().timeout(10, SECONDS);
            taskResourceRepository.save(taskResource);
        });

        TaskResource otherTaskResource = new TaskResource(
            "other task id",
            "other task name",
            "other task type",
            CFTTaskState.ASSIGNED,
            created,
            dueDate,
            priorityDate
        );

        assertDoesNotThrow(() -> taskResourceRepository.insertAndLock(
            otherTaskResource.getTaskId(),
            otherTaskResource.getCreated(),
            otherTaskResource.getDueDateTime(),
            otherTaskResource.getPriorityDate()
        ));

        await()
            .untilAsserted(() -> {
                checkTaskWasSaved(taskResource.getTaskId());
                checkTaskWasSaved(otherTaskResource.getTaskId());
            });

        executorService.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(13, SECONDS);

        assertAll(
            () -> assertTrue(executorService.isShutdown()),
            () -> assertTrue(executorService.isTerminated())
        );
    }

    @Test
    void given_task_is_created_when_find_by_id_then_task_roles_and_work_types_have_expected_values() {

        TaskResource createdTask = createTask(taskId, "tribunal-caseofficer", "IA",
            "startAppeal", "someAssignee", "1623278362430412", CFTTaskState.ASSIGNED);
        assertThat(createdTask.getTaskId()).isEqualTo(taskId);

        final TaskResource taskResource =
            taskResourceRepository.findById(taskId)
                .orElseThrow(
                    () -> new ResourceNotFoundException("Couldn't find the Task created using the id: " + taskId)
                );

        WorkTypeResource workTypeResource = taskResource.getWorkTypeResource();

        assertEquals("routine_work", workTypeResource.getId());
        assertEquals("Routine work", workTypeResource.getLabel());

        final List<NoteResource> notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals(taskId, taskResource.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, taskResource.getExecutionTypeCode().getExecutionCode()),
            () -> assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification()),
            () -> assertEquals(CFTTaskState.ASSIGNED, taskResource.getState()),
            () -> assertEquals(TaskSystem.SELF, taskResource.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, taskResource.getBusinessContext()),
            () -> assertEquals(ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties()),
            () -> assertEquals(
                LocalDate.of(2022, 5, 9),
                taskResource.getAssignmentExpiry().toLocalDate()
            ),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.get(0).getNoteType())
        );

        final List<TaskRoleResource> taskRoleResources =
            taskRoleResourceRepository.findByTaskId(createdTask.getTaskId());

        assertThat(taskRoleResources).isNotEmpty();
        assertThat(taskRoleResources).hasSize(1);

        final TaskRoleResource taskRoleResource = taskRoleResources.get(0);

        String[] expectedAuthorizations = new String[]{"SPECIFIC", "STANDARD"};

        assertAll(
            () -> assertNotNull(taskRoleResource.getTaskRoleId()),
            () -> assertEquals(taskId, taskRoleResource.getTaskId()),
            () -> assertTrue(taskRoleResource.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRoleResource.getRoleName()),
            () -> assertArrayEquals(expectedAuthorizations, taskRoleResource.getAuthorizations())
        );

        assertFalse(taskRoleResource.getComplete());
        assertFalse(taskRoleResource.getCompleteOwn());
        assertFalse(taskRoleResource.getCancelOwn());
        assertFalse(taskRoleResource.getClaim());
        assertFalse(taskRoleResource.getUnclaim());
        assertFalse(taskRoleResource.getAssign());
        assertFalse(taskRoleResource.getUnassign());
        assertFalse(taskRoleResource.getUnclaimAssign());
        assertFalse(taskRoleResource.getUnassignClaim());
        assertFalse(taskRoleResource.getUnassignAssign());
    }

    @Test
    void given_task_is_created_when_find_by_id_and_state_return_tasks_in_the_given_state() {

        String firstTaskId = UUID.randomUUID().toString();
        String secondTaskId = UUID.randomUUID().toString();

        TaskResource firstTask = createTask(firstTaskId, "tribunal-caseofficer", "IA",
                                            "startAppeal", "someAssignee", "1623278362430412",
                                            CFTTaskState.ASSIGNED);
        TaskResource secondTask = createTask(secondTaskId, "tribunal-caseofficer", "IA",
                                              "startAppeal", "someAssignee",
                                             "1623278362430412", CFTTaskState.ASSIGNED);
        taskResourceRepository.save(firstTask);
        secondTask.setState(CFTTaskState.CANCELLED);
        taskResourceRepository.save(secondTask);
        assertThat(firstTask.getTaskId()).isEqualTo(firstTaskId);
        assertThat(secondTask.getTaskId()).isEqualTo(secondTaskId);

        final Optional<TaskResource> taskResource = taskResourceRepository
            .findByIdAndStateIn(firstTaskId, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));
        assertAll(
            () -> assertTrue(taskResource.isPresent()),
            () -> assertEquals(firstTaskId, taskResource.get().getTaskId()),
            () -> assertEquals(CFTTaskState.ASSIGNED, taskResource.get().getState())
        );
        final Optional<TaskResource> taskResource2 =
            taskResourceRepository.findByIdAndStateIn(
                secondTaskId, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        assertFalse(taskResource2.isPresent());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "EXUI_CASE-EVENT_COMPLETION, EXUI_CASE_EVENT_COMPLETION",
        "EXUI_USER_COMPLETION, EXUI_USER_COMPLETION",
        "EXUI_USER_CANCELLATION, EXUI_USER_CANCELLATION",
        "CASE_EVENT_CANCELLATION, EXUI_CASE_EVENT_CANCELLATION",
        "NULL,NULL"
    }, nullValues = "NULL")
    void should_return_termination_process_when_task_is_created_and_by_id(
        String terminationProcess, TerminationProcess expectedTerminationProcess) {
        String taskId = UUID.randomUUID().toString();
        Optional<TerminationProcess> terminationProcessEnum;
        if (terminationProcess != null) {
            terminationProcessEnum = TerminationProcess.fromValue(terminationProcess);
        } else {
            terminationProcessEnum = Optional.empty();
        }

        TaskResource taskResource = createTask(taskId, "tribunal-caseofficer", "IA",
                                               "startAppeal", "someAssignee", "1623278362430412",
                                               CFTTaskState.ASSIGNED);

        taskResource.setTerminationProcess(terminationProcessEnum.orElse(null));

        taskResourceRepository.save(taskResource);

        Optional<TaskResource> taskResourceInDb = taskResourceRepository
            .findById(taskId);
        assertAll(
            () -> assertTrue(taskResourceInDb.isPresent()),
            () -> assertEquals(taskId, taskResourceInDb.get().getTaskId()),
            () -> assertEquals(expectedTerminationProcess, taskResourceInDb.get().getTerminationProcess())
        );

    }

    @Test
    void given_task_is_created_when_search_request_received_then_task_id_is_returned() {
        reindexTasks(taskId);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .caseIds(List.of("1623278362430412"))
            .taskTypes(List.of("startAppeal"))
            .users(List.of("someAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_task_is_indexed_then_task_search_permissions_are_populated() {
        reindexTasks(taskId);

        Boolean searchPermissionsPopulated = jdbcTemplate.queryForObject(
            """
                SELECT EXISTS (
                    SELECT 1
                    FROM cft_task_db.task_search_permissions
                    WHERE task_id = ?
                      AND role_name = 'tribunal-caseofficer'
                      AND permission = 'r'
                      AND authorization_value IS NULL
                )
                """,
            Boolean.class,
            taskId
        );

        assertThat(searchPermissionsPopulated).isTrue();
    }

    @Test
    void given_indexed_task_when_relevant_task_role_changes_then_task_search_permissions_are_refreshed() {
        reindexTasks(taskId);
        TaskRoleResource taskRole = taskRoleResourceRepository.findByTaskId(taskId).get(0);

        transactionHelper.doInNewTransaction(() -> {
            taskRole.setRoleName("case-manager");
            taskRoleResourceRepository.save(taskRole);
        });

        assertThat(searchTasksForRole("IA:*:*:tribunal-caseofficer:*:r:U:*")).isEmpty();
        assertThat(searchTasksForRole("IA:*:*:case-manager:*:r:U:*")).containsExactly(taskId);
    }

    @Test
    void given_exact_role_signature_does_not_match_then_task_is_not_returned() {
        reindexTasks(taskId);

        jdbcTemplate.update(
            """
                UPDATE cft_task_db.task_search_permissions
                SET role_name = ?
                WHERE task_id = ?
                """,
            "case-manager",
            taskId
        );

        assertThat(searchTasksForRole("IA:*:*:tribunal-caseofficer:*:r:U:*")).isEmpty();
    }

    @Test
    void given_search_schema_then_btree_permission_indexes_exist_alongside_legacy_gin_index() {
        reindexTasks(taskId);

        List<String> indexDefinitions = jdbcTemplate.queryForList(
            """
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'cft_task_db'
                  AND indexname IN (
                      'task_search_permissions_auth_idx',
                      'task_search_permissions_lookup_idx',
                      'task_search_permissions_null_auth_idx',
                      'search_active_tasks_sort_idx',
                      'search_assignee_idx',
                      'search_case_id_idx',
                      'search_task_filters_idx',
                      'search_task_type_idx'
                  )
                ORDER BY indexname
                """,
            String.class
        );
        Integer legacyGinIndexCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'cft_task_db'
                  AND indexname = 'search_index'
                """,
            Integer.class
        );
        Integer replacementGinIndexCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'cft_task_db'
                  AND indexname IN (
                      'search_signatures_idx',
                      'search_filter_signature_hashes_idx',
                      'search_role_signature_hashes_idx'
                  )
                """,
            Integer.class
        );
        Integer materialisedSignatureColumnCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'cft_task_db'
                  AND table_name = 'tasks'
                  AND column_name IN (
                      'filter_signatures',
                      'role_signatures',
                      'filter_signature_hashes',
                      'role_signature_hashes'
                  )
                """,
            Integer.class
        );
        Integer materialisedSignatureTriggerCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE tgrelid IN ('cft_task_db.tasks'::regclass, 'cft_task_db.task_roles'::regclass)
                  AND tgname IN (
                      'refresh_task_search_columns_on_tasks',
                      'refresh_task_search_columns_on_task_roles'
                  )
                """,
            Integer.class
        );
        Integer materialisedSignatureFunctionCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM pg_proc proc
                JOIN pg_namespace namespace
                  ON namespace.oid = proc.pronamespace
                WHERE namespace.nspname = 'cft_task_db'
                  AND proc.proname IN (
                      'refresh_task_search_columns',
                      'refresh_task_role_search_columns',
                      'materialise_filter_signatures',
                      'materialise_role_signatures',
                      'signature_hashes',
                      'canonical_signatures'
                  )
                """,
            Integer.class
        );

        assertThat(indexDefinitions)
            .hasSize(8)
            .allSatisfy(index -> assertThat(index).contains("USING btree"));
        assertThat(legacyGinIndexCount).isOne();
        assertThat(replacementGinIndexCount).isZero();
        assertThat(materialisedSignatureColumnCount).isZero();
        assertThat(materialisedSignatureTriggerCount).isZero();
        assertThat(materialisedSignatureFunctionCount).isZero();
    }

    @Test
    void given_multiple_tasks_created_when_search_request_received_then_task_ids_are_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT)))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(2, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
        assertEquals(taskId2, taskIds.get(1));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_case_id() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED))
            .caseIds(List.of("1623278362430412"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_task_type() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_assignee() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_state() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_filter_signature() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "WA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.ASSIGNED);
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:WA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "WA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId2, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_received_then_task_ids_are_returned_and_filter_by_role_signature() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "WA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.ASSIGNED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:WA:*:*:1:765324", "*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("WA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId2, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_for_available_task_then_task_ids_without_assignee_are_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", null, "1623278362430413", CFTTaskState.UNASSIGNED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(1, taskIds.size());
        assertEquals(taskId2, taskIds.get(0));
    }

    @Test
    void given_tasks_created_when_search_request_without_state_filter_then_return_only_assigned_and_unassigned_tasks() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.UNASSIGNED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT)))
            .build();

        List<String> taskIds = searchTasks(filterSignature, roleSignature, List.of(), request);
        assertEquals(2, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
        assertEquals(taskId2, taskIds.get(1));
    }

    @Test
    void given_tasks_created_when_search_request_with_excluded_case_id_then_tasks_from_excluded_id_is_not_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        reindexTasks(taskId, taskId2);

        Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
        Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*");
        SearchRequest request = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED))
            .caseIds(List.of("1623278362430412", "1623278362430413"))
            .taskTypes(List.of("startAppeal", "reviewAppeal"))
            .users(List.of("someAssignee", "anotherAssignee"))
            .build();

        List<String> taskIds = searchTasks(
            filterSignature,
            roleSignature,
            List.of("1623278362430413", "88888888888888"),
            request
        );
        assertEquals(1, taskIds.size());
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_multiple_tasks_created_when_find_by_task_ids_then_return_ordered_task_list() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "tribunal-caseofficer", "IA",
            "startAppeal", "someAssignee", "1623278362430412", CFTTaskState.ASSIGNED);
        createdTask.setCaseName("TestCaseB");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        Sort sort = Sort.by(Sort.Order.asc("caseName"));
        List<TaskResource> tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId), sort);
        assertEquals(1, tasksResult.size());

        sort = Sort.by(Sort.Order.asc("caseName"));
        tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2), sort);
        assertEquals(2, tasksResult.size());
        assertEquals(taskId, tasksResult.get(0).getTaskId());
        assertEquals(taskId2, tasksResult.get(1).getTaskId());
        assertEquals("TestCaseA", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(1).getCaseName());

        sort = Sort.by(Sort.Order.desc("caseName"));
        tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2), sort);
        assertEquals(2, tasksResult.size());
        assertEquals(taskId2, tasksResult.get(0).getTaskId());
        assertEquals(taskId, tasksResult.get(1).getTaskId());
        assertEquals("TestCaseB", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseA", tasksResult.get(1).getCaseName());
    }

    @Test
    void given_multiple_tasks_created_when_find_task_with_multiple_order_filed_then_return_ordered_task_list() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "tribunal-caseofficer", "IA",
            "startAppeal", "someAssignee", "1623278362430412", CFTTaskState.ASSIGNED);
        createdTask.setCaseName("TestCaseB");
        createdTask.setCaseCategory("caseCategoryB");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        String taskId3 = UUID.randomUUID().toString();
        TaskResource createdTask2 = createTask(taskId3, "tribunal-caseofficer", "IA",
            "startAppeal", "someAssignee", "1623278362430412", CFTTaskState.ASSIGNED);
        createdTask2.setCaseName("TestCaseB");
        createdTask2.setCaseCategory("caseCategoryC");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask2));

        Sort sort = Sort.by(Sort.Order.asc("caseName"),
            Sort.Order.desc("caseCategory"));
        List<TaskResource> tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2, taskId3),
            sort);

        assertEquals(3, tasksResult.size());
        assertEquals(taskId, tasksResult.get(0).getTaskId());
        assertEquals(taskId3, tasksResult.get(1).getTaskId());
        assertEquals(taskId2, tasksResult.get(2).getTaskId());
        assertEquals("TestCaseA", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(1).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(2).getCaseName());
        assertEquals("caseCategoryA", tasksResult.get(0).getCaseCategory());
        assertEquals("caseCategoryC", tasksResult.get(1).getCaseCategory());
        assertEquals("caseCategoryB", tasksResult.get(2).getCaseCategory());
    }

    @Test
    void given_tasks_exist_with_index_flag_false_when_find_by_index_false_then_tasks_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.ASSIGNED);

        transactionHelper.doInNewTransaction(() -> {
            task.setIndexed(false);
            createdTask.setIndexed(false);
            taskResourceRepository.save(task);
            taskResourceRepository.save(createdTask);
        });

        List<TaskResource> taskResult = taskResourceRepository
            .findByIndexedFalseAndStateIn(List.of(CFTTaskState.ASSIGNED));
        assertNotNull(taskResult);
        assertEquals(2, taskResult.size());
        assertThat(List.of(taskId2, taskId)).contains(taskResult.get(0).getTaskId());
        assertThat(List.of(taskId2, taskId)).contains(taskResult.get(1).getTaskId());
    }

    @Test
    void given_tasks_exist_with_index_flag_true_when_find_by_index_false_then_tasks_not_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.UNASSIGNED);

        transactionHelper.doInNewTransaction(() -> {
            task.setIndexed(true);
            createdTask.setIndexed(true);
            taskResourceRepository.save(task);
            taskResourceRepository.save(createdTask);
        });

        List<TaskResource> taskResult = taskResourceRepository
            .findByIndexedFalseAndStateIn(List.of(CFTTaskState.UNASSIGNED));

        assertNotNull(taskResult);
        assertEquals(0, taskResult.size());
    }

    @Test
    void given_tasks_exist_with_index_flag_true_when_find_by_index_true_state_completed_then_tasks_not_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);

        transactionHelper.doInNewTransaction(() -> {
            task.setIndexed(false);
            createdTask.setIndexed(false);
            taskResourceRepository.save(task);
            taskResourceRepository.save(createdTask);
        });

        List<TaskResource> taskResult = taskResourceRepository
            .findByIndexedFalseAndStateIn(List.of(CFTTaskState.UNASSIGNED));

        assertNotNull(taskResult);
        assertEquals(0, taskResult.size());
    }


    @Test
    void given_tasks_exist_when_get_top_5_order_by_lastUpdated_timestamp_asc_then_return_max_5_tasks_ordered() {
        taskResourceRepository.deleteAll();
        List<TaskResource> createdTasks = new ArrayList<>(6);
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")));
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:46.345875+01:00")));
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:47.345875+01:00")));
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:48.345875+01:00")));
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:49.345875+01:00")));
        createdTasks.add(createTask(UUID.randomUUID().toString(),
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00")));

        taskResourceRepository.saveAll(createdTasks);

        List<TaskResource> results = taskResourceRepository.findTop5ByOrderByLastUpdatedTimestampDesc();
        assertEquals(5, results.size());
        assertTrue(results.get(0).getLastUpdatedTimestamp()
            .isEqual(OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00")));
        assertTrue(results.get(1).getLastUpdatedTimestamp()
            .isEqual(OffsetDateTime.parse("2021-05-09T20:15:49.345875+01:00")));
        assertTrue(results.get(2).getLastUpdatedTimestamp()
            .isEqual(OffsetDateTime.parse("2021-05-09T20:15:48.345875+01:00")));
        assertTrue(results.get(3).getLastUpdatedTimestamp()
            .isEqual(OffsetDateTime.parse("2021-05-09T20:15:47.345875+01:00")));
        assertTrue(results.get(4).getLastUpdatedTimestamp()
            .isEqual(OffsetDateTime.parse("2021-05-09T20:15:46.345875+01:00")));

    }

    private void checkTaskWasSaved(String taskId) {
        assertTrue(taskResourceRepository.getByTaskId(taskId).isPresent());
    }

    private void reindexTasks(String... taskIds) {
        transactionHelper.doInNewTransaction(() -> {
            for (String id : taskIds) {
                TaskResource taskToIndex = taskResourceRepository.findById(id).orElseThrow();
                taskToIndex.setIndexed(true);
                taskResourceRepository.save(taskToIndex);
            }
        });
    }

    private List<String> searchTasksForRole(String roleSignature) {
        return searchTasks(
            Set.of("*:IA:*:*:1:765324"),
            Set.of(roleSignature),
            List.of(),
            SearchRequest.builder().build()
        );
    }

    private List<String> searchTasks(Set<String> filterSignatures,
                                     Set<String> roleSignatures,
                                     List<String> excludeCaseIds,
                                     SearchRequest searchRequest) {
        return taskResourceRepository.searchTasksIds(
            0,
            25,
            toRoleCriteria(roleSignatures),
            excludeCaseIds,
            withFilterCriteria(searchRequest, filterSignatures)
        );
    }

    private SearchRequest withFilterCriteria(SearchRequest searchRequest, Set<String> filterSignatures) {
        SearchFilterCriteria filterCriteria = SearchFilterCriteria.from(filterSignatures);
        return SearchRequest.builder()
            .cftTaskStates(searchRequest.getCftTaskStates())
            .jurisdictions(defaultIfEmpty(searchRequest.getJurisdictions(), filterCriteria.jurisdictions()))
            .locations(defaultIfEmpty(searchRequest.getLocations(), filterCriteria.locations()))
            .regions(defaultIfEmpty(searchRequest.getRegions(), filterCriteria.regions()))
            .caseIds(searchRequest.getCaseIds())
            .users(searchRequest.getUsers())
            .taskTypes(searchRequest.getTaskTypes())
            .workTypes(defaultIfEmpty(searchRequest.getWorkTypes(), filterCriteria.workTypes()))
            .roleCategories(defaultIfEmpty(searchRequest.getRoleCategories(), filterCriteria.roleCategories()))
            .requestContext(requestContext(searchRequest))
            .sortingParameters(searchRequest.getSortingParameters())
            .build();
    }

    private <T> List<T> defaultIfEmpty(List<T> existingValues, List<T> defaultValues) {
        return existingValues == null || existingValues.isEmpty() ? defaultValues : existingValues;
    }

    private RequestContext requestContext(SearchRequest searchRequest) {
        if (searchRequest.isAvailableTasksOnly()) {
            return RequestContext.AVAILABLE_TASKS;
        } else if (searchRequest.isAllWork()) {
            return RequestContext.ALL_WORK;
        }
        return null;
    }

    private List<TaskSearchRoleCriteria> toRoleCriteria(Set<String> roleSignatures) {
        return roleSignatures.stream()
            .map(this::toRoleCriteria)
            .toList();
    }

    private TaskSearchRoleCriteria toRoleCriteria(String roleSignature) {
        String[] parts = roleSignature.split(":", 8);
        return new TaskSearchRoleCriteria(
            nullIfWildcard(parts[0]),
            nullIfWildcard(parts[1]),
            nullIfWildcard(parts[2]),
            parts[3],
            nullIfWildcard(parts[4]),
            parts[5],
            parts[6],
            nullIfWildcard(parts[7])
        );
    }

    private String nullIfWildcard(String value) {
        return "*".equals(value) ? null : value;
    }

    private record SearchFilterCriteria(List<String> jurisdictions,
                                        List<RoleCategory> roleCategories,
                                        List<String> workTypes,
                                        List<String> regions,
                                        List<String> locations) {

        private static SearchFilterCriteria from(Set<String> filterSignatures) {
            Set<String> jurisdictions = new LinkedHashSet<>();
            Set<RoleCategory> roleCategories = new LinkedHashSet<>();
            Set<String> workTypes = new LinkedHashSet<>();
            Set<String> regions = new LinkedHashSet<>();
            Set<String> locations = new LinkedHashSet<>();

            for (String filterSignature : filterSignatures) {
                String[] parts = filterSignature.split(":", -1);
                addIfConstrained(jurisdictions, parts[1]);
                if (!"*".equals(parts[2])) {
                    roleCategories.add(expandRoleCategory(parts[2]));
                }
                addIfConstrained(workTypes, parts[3]);
                addIfConstrained(regions, parts[4]);
                addIfConstrained(locations, parts[5]);
            }

            return new SearchFilterCriteria(
                List.copyOf(jurisdictions),
                List.copyOf(roleCategories),
                List.copyOf(workTypes),
                List.copyOf(regions),
                List.copyOf(locations)
            );
        }

        private static void addIfConstrained(Set<String> values, String value) {
            if (!"*".equals(value)) {
                values.add(value);
            }
        }

        private static RoleCategory expandRoleCategory(String value) {
            return switch (value) {
                case "J" -> RoleCategory.JUDICIAL;
                case "L" -> RoleCategory.LEGAL_OPERATIONS;
                case "A" -> RoleCategory.ADMIN;
                case "C" -> RoleCategory.CTSC;
                case "E" -> RoleCategory.ENFORCEMENT;
                default -> throw new IllegalArgumentException("Invalid filter role category: " + value);
            };
        }
    }

    private TaskResource createTask(String taskId, OffsetDateTime lastUpdated) {
        TaskResource createdTask = createTask(taskId, "case-manager", "IA",
            "reviewAppeal", "anotherAssignee", "1623278362430413", CFTTaskState.COMPLETED);
        createdTask.setLastUpdatedTimestamp(lastUpdated);
        return createdTask;
    }

    private TaskResource createTask(String taskId, String roleName, String jurisdiction, String taskType,
                                    String assignee, String caseId, CFTTaskState state) {
        List<NoteResource> notes = singletonList(
            new NoteResource(
                "someCode",
                "noteTypeVal",
                "userVal",
                "someContent"
            ));
        return new TaskResource(
            taskId,
            "aTaskName",
            taskType,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            state,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            notes,
            0,
            0,
            assignee,
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            caseId,
            "Asylum",
            "TestCaseA",
            jurisdiction,
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(new TaskRoleResource(
                roleName,
                true,
                false,
                false,
                false,
                false,
                false,
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            )),
            "caseCategoryA",
            ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

}
