package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.launchdarkly.shaded.com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/search_tasks_data.sql")
class CFTTaskDatabaseServiceSearchTest extends RoleAssignmentHelper {
    private static final UserInfo userInfo = UserInfo.builder().email("user@test.com").uid("user").build();

    @Autowired
    TaskResourceRepository taskResourceRepository;

    CFTTaskDatabaseService cftTaskDatabaseService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);
    }

    // ** Filter Signature **
    //STATE("state"),
    //JURISDICTION("jurisdiction"),
    //LOCATION("location"),
    //ROLE_CATEGORY("role_category");
    //WORK_TYPE("work_type"),

    // ** Role Signature **
    //AVAILABLE_TASKS_ONLY("available_tasks_only"),

    // ** Extra Param **
    //USER("user"),
    //TASK_TYPE("task_type"),
    //CASE_ID("case_id"),
    //CASE_ID_CAMEL_CASE("caseId"),


    @Test
    void should_return_ordered_by_asc_task_list_and_count_when_search_find_some_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001",
            "8d6cc5cf-c973-11eb-aaaa-100000000001",
            "8d6cc5cf-c973-11eb-aaaa-200000000001",
            "8d6cc5cf-c973-11eb-aaaa-300000000001",
            "8d6cc5cf-c973-11eb-aaaa-400000000001",
            "8d6cc5cf-c973-11eb-aaaa-000000000002",
            "8d6cc5cf-c973-11eb-aaaa-000000000003",
            "8d6cc5cf-c973-11eb-aaaa-000000000004");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324", "765325"))
            .roleCategories(List.of(RoleCategory.JUDICIAL, RoleCategory.CTSC))
            .workTypes(List.of("hearing_work", "follow_up"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse, false);
        assertEquals(8, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(8)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-100000000001", "1623278362410001",
                    "8d6cc5cf-c973-11eb-aaaa-200000000001", "1623278362420001",
                    "8d6cc5cf-c973-11eb-aaaa-300000000001", "1623278362430001",
                    "8d6cc5cf-c973-11eb-aaaa-400000000001", "1623278362440001"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_by_decs_task_list_and_count_when_search_find_some_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001",
            "8d6cc5cf-c973-11eb-aaaa-100000000001",
            "8d6cc5cf-c973-11eb-aaaa-200000000001",
            "8d6cc5cf-c973-11eb-aaaa-300000000001",
            "8d6cc5cf-c973-11eb-aaaa-400000000001",
            "8d6cc5cf-c973-11eb-aaaa-000000000002",
            "8d6cc5cf-c973-11eb-aaaa-000000000003",
            "8d6cc5cf-c973-11eb-aaaa-000000000004");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324", "765325"))
            .roleCategories(List.of(RoleCategory.JUDICIAL, RoleCategory.CTSC))
            .workTypes(List.of("hearing_work", "follow_up"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse, false);
        response.getTasks().forEach(t -> System.out.println(t.getId()));

        assertEquals(8, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(8)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-400000000001", "1623278362440001",
                    "8d6cc5cf-c973-11eb-aaaa-300000000001", "1623278362430001",
                    "8d6cc5cf-c973-11eb-aaaa-200000000001", "1623278362420001",
                    "8d6cc5cf-c973-11eb-aaaa-100000000001", "1623278362410001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_state() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001", "8d6cc5cf-c973-11eb-aaaa-000000000002");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .jurisdictions(List.of("WA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse, false);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_jurisdiction() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001", "8d6cc5cf-c973-11eb-aaaa-100000000001");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .jurisdictions(List.of("WA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse, false);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_role_category() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001", "8d6cc5cf-c973-11eb-aaaa-200000000001");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse, false);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_location() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001", "8d6cc5cf-c973-11eb-aaaa-300000000001");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse,
            false);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_work_type() {
        List<RoleAssignment> roleAssignments = roleAssignmentsWithGrantTypeStandard();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord("8d6cc5cf-c973-11eb-aaaa-000000000001", "8d6cc5cf-c973-11eb-aaaa-400000000001");

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .jurisdictions(List.of("IA", "WA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(searchRequest,
            accessControlResponse,
            false);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                ).toArray()
            );
    }

    private void indexRecord(String... ids) {
        Arrays.stream(ids).forEach(id -> {
            Optional<TaskResource> taskResource = taskResourceRepository.findById(id);
            TaskResource task = taskResource.get();
            task.setIndexed(true);
            taskResourceRepository.save(task);
        });
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeStandard() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(SSCS_JURISDICTION)
                    .region("1")
                    .baseLocation("653255")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }
}
