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
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.IA_JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.PRIMARY_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.WA_JURISDICTION;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/search_tasks_data.sql")
class CFTTaskDatabaseServiceSearchTest {
    private static final UserInfo userInfo = UserInfo.builder().email("user@test.com").uid("user").build();

    @Autowired
    TaskResourceRepository taskResourceRepository;

    CFTTaskDatabaseService cftTaskDatabaseService;

    static RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

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
    //JURISDICTION,
    //REGION,
    //BASE_LOCATION,
    //CASE_ID,
    //classification,
    //roleAssignment.getRoleName()
    //permission
    //authorisation

    // ** Extra Param **
    //USER("user"), as assignee
    //USER("user"), as assignee is null for available task only
    //TASK_TYPE("task_type"),
    //CASE_ID("case_id") or CASE_ID_CAMEL_CASE("caseId")
    //CASE_ID("case_id") excluded case_id in role assignments


    @Test
    void should_return_ordered_by_asc_task_list_and_count_when_search_find_some_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324", "765325"))
            .roleCategories(List.of(RoleCategory.JUDICIAL, RoleCategory.CTSC))
            .workTypes(List.of("hearing_work", "follow_up"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
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
    void should_return_ordered_by_desc_task_list_and_count_when_search_find_some_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .locations(List.of("765324", "765325"))
            .roleCategories(List.of(RoleCategory.JUDICIAL, RoleCategory.CTSC))
            .workTypes(List.of("hearing_work", "follow_up"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

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
    void should_return_ordered_by_task_id_asc_task_list_for_same_case_name_when_search_find_some_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA"))
            .locations(List.of("765324"))
            .roleCategories(List.of(RoleCategory.JUDICIAL))
            .workTypes(List.of("hearing_work"))
            .caseIds(List.of("1623278362400003", "1623278362400004"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
                                                                                accessControlResponse);

        assertEquals(2, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(2)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_state() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(3, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(3)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getTaskState)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003", "assigned",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001", "assigned",
                    "8d6cc5cf-c973-11eb-aaaa-100000000001", "1623278362410001", "assigned"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_jurisdiction() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(4, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(4)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getJurisdiction)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003", "WA",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004", "WA",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001", "WA",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002", "WA"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_role_category() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .roleCategories(List.of(RoleCategory.CTSC))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getRoleCategory)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-200000000001", "1623278362420001", "CTSC"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_location() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .locations(List.of("765324"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(4, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(4)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getLocation)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003", "765324",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004", "765324",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001", "765324",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002", "765324"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_and_count_when_filter_task_by_work_type() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .workTypes(List.of("follow_up"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getWorkTypeId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-400000000001", "1623278362440001", "follow_up"
                ).toArray()
            );
    }

    @Test
    void should_return_task_list_filtered_by_role_assignment() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
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
    void should_return_ordered_task_list_and_count_when_search_with_case_role_assignment() {
        List<RoleAssignment> roleAssignments = roleAssignmentsForCaseType();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000014", "1623278362400014"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_available_task_only() {
        List<RoleAssignment> roleAssignments = roleAssignmentsForSeniorTribunalCaseworker();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .cftTaskStates(List.of(CFTTaskState.UNASSIGNED))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_available_task_only_and_filter_by_authorisation() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .cftTaskStates(List.of(CFTTaskState.UNASSIGNED))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-400000000001", "1623278362440001"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_all_work() {
        List<RoleAssignment> roleAssignments = roleAssignmentsForSeniorTribunalCaseworker();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.ALL_WORK)
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.DESCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

        assertEquals(1, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_assignee() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .users(List.of("USER1", "USER2"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(2, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(2)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_case_id() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .caseIds(List.of("1623278362400004", "1623278362400003"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(2, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(2)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_when_search_for_task_type() {
        List<RoleAssignment> roleAssignments = roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .taskTypes(List.of("reviewAppeal"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);
        assertEquals(2, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(2)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getType)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001", "reviewAppeal",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002", "reviewAppeal"
                ).toArray()
            );
    }

    @Test
    void should_return_ordered_task_list_and_count_and_filter_out_tasks_from_excluded_tasks() {
        List<RoleAssignment> roleAssignments = roleAssignmentsForExclusion();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        indexRecord();

        SearchRequest searchRequest = SearchRequest.builder()
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
            .build();


        GetTasksResponse<Task> response = cftTaskDatabaseService.searchForTasks(0, 25, searchRequest,
            accessControlResponse);

        response.getTasks().forEach(t -> System.out.println(t.getId()));

        assertEquals(5, response.getTotalRecords());
        Assertions.assertThat(response.getTasks())
            .hasSize(5)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000035", "1623278362400035",
                    "8d6cc5cf-c973-11eb-aaaa-000000000036", "1623278362400036",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                ).toArray()
            );
    }

    private void indexRecord() {
        String[] ids = {
            "8d6cc5cf-c973-11eb-aaaa-000000000001","8d6cc5cf-c973-11eb-aaaa-100000000001",
            "8d6cc5cf-c973-11eb-aaaa-200000000001","8d6cc5cf-c973-11eb-aaaa-300000000001",
            "8d6cc5cf-c973-11eb-aaaa-400000000001","8d6cc5cf-c973-11eb-aaaa-000000000002",
            "8d6cc5cf-c973-11eb-aaaa-000000000003","8d6cc5cf-c973-11eb-aaaa-000000000004",
            "8d6cc5cf-c973-11eb-aaaa-000000000005","8d6cc5cf-c973-11eb-aaaa-000000000006",
            "8d6cc5cf-c973-11eb-aaaa-000000000007","8d6cc5cf-c973-11eb-aaaa-000000000008",
            "8d6cc5cf-c973-11eb-aaaa-000000000009","8d6cc5cf-c973-11eb-aaaa-000000000010",
            "8d6cc5cf-c973-11eb-aaaa-000000000011","8d6cc5cf-c973-11eb-aaaa-000000000012",
            "8d6cc5cf-c973-11eb-aaaa-000000000013","8d6cc5cf-c973-11eb-aaaa-000000000014",
            "8d6cc5cf-c973-11eb-aaaa-000000000015","8d6cc5cf-c973-11eb-aaaa-000000000016",
            "8d6cc5cf-c973-11eb-aaaa-000000000017","8d6cc5cf-c973-11eb-aaaa-000000000018",
            "8d6cc5cf-c973-11eb-aaaa-000000000019","8d6cc5cf-c973-11eb-aaaa-000000000020",
            "8d6cc5cf-c973-11eb-aaaa-000000000021","8d6cc5cf-c973-11eb-aaaa-000000000022",
            "8d6cc5cf-c973-11eb-aaaa-000000000023","8d6cc5cf-c973-11eb-aaaa-000000000024",
            "8d6cc5cf-c973-11eb-aaaa-000000000025","8d6cc5cf-c973-11eb-aaaa-000000000026",
            "8d6cc5cf-c973-11eb-aaaa-000000000027","8d6cc5cf-c973-11eb-aaaa-000000000028",
            "8d6cc5cf-c973-11eb-aaaa-000000000029","8d6cc5cf-c973-11eb-aaaa-000000000030",
            "8d6cc5cf-c973-11eb-aaaa-000000000031","8d6cc5cf-c973-11eb-aaaa-000000000032",
            "8d6cc5cf-c973-11eb-aaaa-000000000033","8d6cc5cf-c973-11eb-aaaa-000000000034",
            "8d6cc5cf-c973-11eb-aaaa-000000000035","8d6cc5cf-c973-11eb-aaaa-000000000036",
            "8d6cc5cf-c973-11eb-aaaa-000000000040","8d6cc5cf-c973-11eb-aaaa-000000000041",
            "8d6cc5cf-c973-11eb-aaaa-000000000042","8d6cc5cf-c973-11eb-aaaa-000000000043",
            "8d6cc5cf-c973-11eb-aaaa-000000000044","8d6cc5cf-c973-11eb-aaaa-000000000045"
        };
        Arrays.stream(ids).forEach(id -> {
            Optional<TaskResource> taskResource = taskResourceRepository.findById(id);
            TaskResource task = taskResource.get();
            task.setIndexed(true);
            taskResourceRepository.save(task);
        });
    }

    private static List<RoleAssignment> roleAssignmentsForCaseType() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.SPECIFIC_LEAD_JUDGE_RESTRICTED
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .caseId("1623278362400014")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsForSeniorTribunalCaseworker() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_PUBLIC
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsTribunalCaseWorkerWithPublicAndPrivateClasification() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PUBLIC.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(IA_JURISDICTION)
                    .region("2")
                    .baseLocation("765325")
                    .build()
            )
            .authorisations(List.of("skill2"))
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + Classification.PRIVATE.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);



        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsForExclusion() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_PUBLIC
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_LEGAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .caseId("1623278362400044")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_LEGAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .caseId("1623278362400045")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }
}
