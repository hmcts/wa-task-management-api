package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.wa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterRequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static com.launchdarkly.shaded.com.google.common.collect.Lists.newArrayList;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.IN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.REQUEST_CONTEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/search_tasks_data.sql")
public class CftQueryServiceITTest extends RoleAssignmentHelper {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private CftQueryService cftQueryService;

    private static final UserInfo userInfo = UserInfo.builder().email("user@test.com").uid("user").build();
    private static final UserInfo granularPermissionUserInfo = UserInfo.builder()
        .email("granular_user@test.com")
        .uid("granular_user").build();

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(
            camundaService,
            cftTaskMapper,
            new TaskResourceDao(entityManager),
            allowedJurisdictionConfiguration,
            launchDarklyFeatureFlagProvider
        );

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            userInfo.getUid(),
            userInfo.getEmail()
        )).thenReturn(false);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            granularPermissionUserInfo.getUid(),
            granularPermissionUserInfo.getEmail()
        )).thenReturn(true);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeSpecificScenarioHappyPath",
        "grantTypeStandardScenarioHappyPath",
        "grantTypeChallengedScenarioHappyPath",
        "grantTypeWithStandardAndExcludedScenarioHappyPath",
        "grantTypeWithChallengedAndExcludedScenarioHappyPath",
        "grantTypeWithAvailableTasksOnlyScenarioHappyPath",
        "inActiveRole",
        "sortByFieldScenario",
        "paginatedResultsScenario",
        "searchByWorkTypeScenario",
        "grantTypeWithAvailableTasksOnlyRequestContextScenarioHappyPath",
        "grantTypeWithAllWorkRequestContextScenarioHappyPath",
        "searchByWorkTypeScenario",
        "searchByCaseIdScenario",
        "searchByJurisdictionLocationAndStateScenario",
        "searchByRoleCategoryScenario",
        "searchByStateScenario",
        "searchByJurisdictionAndLocationScenario"
    })
    void should_retrieve_tasks(TaskQueryScenario scenario) {

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
            scenario.roleAssignments);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse,
            false
        );
        
        //then
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(scenario.expectedTotalRecords);
        //then
        if (scenario.expectedTotalRecords > 0) {
            Assertions.assertThat(allTasks.getTasks())
                .hasSize(scenario.expectedAmountOfTasksInResponse)
                .flatExtracting(Task::getId, Task::getCaseId)
                .containsExactly(
                    scenario.expectedTaskDetails.toArray()
                );
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeWithStandardAndExcludedScenarioHappyPath"
    })
    void should_retrieve_tasks_with_non_granular_permission(TaskQueryScenario scenario) {

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
                                                                                scenario.roleAssignments);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse,
            false
        );
        //then
        Assertions.assertThat(allTasks.getTasks().get(0).getPermissions().getValues().contains(
            READ)).isTrue();
        Assertions.assertThat(allTasks.getTasks().get(0).getPermissions().getValues().contains(
            UNCLAIM_ASSIGN)).isFalse();

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeWithStandardAndExcludedScenarioHappyPath"
    })
    void should_retrieve_tasks_with_granular_permission(TaskQueryScenario scenario) {

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
                                                                                scenario.roleAssignments);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse,
            true
        );

        //then
        Assertions.assertThat(allTasks.getTasks().get(0).getPermissions().getValues().containsAll(
            List.of(READ, UNASSIGN, UNCLAIM_ASSIGN))).isTrue();

    }


    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "sortByFieldNexHearingDateAscOrder",
        "sortByFieldNexHearingDateDescOrder"
    })
    void should_retrieve_tasks_ordered_on_next_hearing_date(TaskQueryScenario scenario) {

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
            scenario.roleAssignments);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse,
            false
        );

        //then
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(scenario.expectedTotalRecords);
        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedAmountOfTasksInResponse)
            .flatExtracting(Task::getId, Task::getCaseId,
                t -> t.getNextHearingDate().toOffsetDateTime().format(DATE_TIME_FORMATTER)
            )
            .containsExactly(
                scenario.expectedTaskDetails.toArray()
            );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeSpecificErrorScenario",
        "grantTypeStandardErrorScenario",
        "grantTypeChallengedErrorScenario",
        "grantTypeWithStandardAndExcludedErrorScenario",
        "grantTypeWithChallengedAndExcludedErrorScenario",
        "inValidBeginAndEndTime"
    })
    void should_return_empty_list_when_search_request_is_invalid(TaskQueryScenario scenario) {

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
            scenario.roleAssignments);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse,
            false
        );

        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedAmountOfTasksInResponse);
    }

    @Test
    void handle_pagination_error() {

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            userInfo,
            List.of(RoleAssignment.builder().build())
        );

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(
                new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)
            )
        );

        Assertions.assertThatThrownBy(() -> cftQueryService.searchForTasks(
                -1,
                1,
                searchTaskRequest,
                accessControlResponse,
                false
            ))
            .hasNoCause()
            .hasMessage("Offset index must not be less than zero");


        Assertions.assertThatThrownBy(() -> cftQueryService.searchForTasks(
                0,
                0,
                searchTaskRequest,
                accessControlResponse,
                false
            ))
            .hasNoCause()
            .hasMessage("Limit must not be less than one");
    }

    @Test
    void should_not_retrieve_tasks_when_role_assignment_standard_and_excluded() {

        String caseId = "1623278362400007";
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(caseId))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //apply standard role to user
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

        GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );


        //standard user can retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(1);

        Assertions.assertThat(allTasks.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getCaseId)
            .containsExactly(caseId);


        //apply excluded role to standard user
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

        allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );

        //when excluded role applied to standard user can not retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(0);

    }

    @Test
    void should_not_retrieve_tasks_when_role_assignment_challenged_and_excluded() {

        String caseId = "1623278362400015";
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(caseId))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //apply challenged role to user
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .caseId(caseId)
                    .build()
            )
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        permissionsRequired.add(READ);

        GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );


        //standard user can retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(1);

        Assertions.assertThat(allTasks.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getCaseId)
            .containsExactly(caseId);


        //apply excluded role to standard user
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        permissionsRequired.add(READ);

        allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );

        //when excluded role applied to challenged user can not retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(0);

    }

    @Test
    void should_retrieve_tasks_when_role_assignment_specific_and_excluded() {

        String caseId = "1623278362400013";
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(caseId))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //apply standard role to user
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_LEAD_JUDGE_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);


        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        permissionsRequired.add(READ);

        GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );


        //standard user can retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(1);

        Assertions.assertThat(allTasks.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getCaseId)
            .containsExactly(caseId);


        //apply excluded role to standard user
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);
        permissionsRequired.add(READ);

        allTasks = cftQueryService.searchForTasks(
            0,
            10,
            searchTaskRequest,
            accessControlResponse,
            false
        );

        //when excluded role applied to specific user can retrieve task
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(1);

        Assertions.assertThat(allTasks.getTasks())
            .hasSize(1)
            .flatExtracting(Task::getCaseId)
            .containsExactly(caseId);

    }

    private static Stream<TaskQueryScenario> grantTypeStandardScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ));


        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(6)
            .expectedTotalRecords(6)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeStandardErrorScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_CASE_WORKER_RESTRICTED)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .authorisations(List.of("unknownValue"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();


        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            withAuthorizations,
            invalidJurisdiction,
            invalidLocation,
            invalidCaseId,
            invalidUser
        );
    }

    private static Stream<TaskQueryScenario> grantTypeSpecificScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("1623278362400013"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000013", "1623278362400013"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000013", "1623278362400013"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000013", "1623278362400013"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeSpecificErrorScenario() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(invalidCaseRoleAssignmentsWithGrantTypeSpecific())
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            withAuthorizations,
            invalidJurisdiction,
            invalidLocation,
            invalidCaseId,
            invalidUser
        );
    }

    private static Stream<TaskQueryScenario> grantTypeChallengedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015",
                    "8d6cc5cf-c973-11eb-aaaa-000000000016", "1623278362400016"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(4)
            .expectedTotalRecords(4)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015",
                    "8d6cc5cf-c973-11eb-aaaa-000000000017", "1623278362400017",
                    "8d6cc5cf-c973-11eb-aaaa-000000000016", "1623278362400016",
                    "8d6cc5cf-c973-11eb-aaaa-000000000018", "1623278362400018"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(6)
            .expectedTotalRecords(6)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015",
                    "8d6cc5cf-c973-11eb-aaaa-000000000017", "1623278362400017",
                    "8d6cc5cf-c973-11eb-aaaa-000000000019", "1623278362400019",
                    "8d6cc5cf-c973-11eb-aaaa-000000000016", "1623278362400016",
                    "8d6cc5cf-c973-11eb-aaaa-000000000018", "1623278362400018",
                    "8d6cc5cf-c973-11eb-aaaa-000000000020", "1623278362400020"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeChallengedErrorScenario() {

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();


        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();


        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            invalidJurisdiction,
            invalidLocation,
            invalidCaseId,
            invalidUser
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithStandardAndExcludedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(4)
            .expectedTotalRecords(4)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042"

                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithStandardAndExcludedErrorScenario() {

        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_CASE_WORKER_RESTRICTED)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .build()
            )
            .authorisations(List.of("unknownValue"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .caseId("1623278362400001")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final TaskQueryScenario invalidAuthorization = TaskQueryScenario.builder()
            .scenarioName("standard_and_excluded_grant_type_with_invalid_authorization")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            invalidAuthorization
        );

    }

    private static Stream<TaskQueryScenario> grantTypeWithChallengedAndExcludedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015",
                    "8d6cc5cf-c973-11eb-aaaa-000000000017", "1623278362400017",
                    "8d6cc5cf-c973-11eb-aaaa-000000000018", "1623278362400018"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(5)
            .expectedTotalRecords(5)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000015", "1623278362400015",
                    "8d6cc5cf-c973-11eb-aaaa-000000000017", "1623278362400017",
                    "8d6cc5cf-c973-11eb-aaaa-000000000019", "1623278362400019",
                    "8d6cc5cf-c973-11eb-aaaa-000000000018", "1623278362400018",
                    "8d6cc5cf-c973-11eb-aaaa-000000000020", "1623278362400020"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithChallengedAndExcludedErrorScenario() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(SSCS_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .build()
            )
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .caseId("1623278362400001")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_and_excluded_grant_type_with_invalid_caseId")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            invalidCaseId
        );

    }

    private static Stream<TaskQueryScenario> grantTypeWithAvailableTasksOnlyScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN permission and PRIVATE")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(4)
            .expectedTotalRecords(4)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(6)
            .expectedTotalRecords(6)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithAvailableTasksOnlyRequestContextScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
            new SearchParameterRequestContext(REQUEST_CONTEXT, SearchOperator.BOOLEAN,
                RequestContext.AVAILABLE_TASK_ONLY)
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN and CLAIM permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN and CLAIM permission and PRIVATE")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN and CLAIM and "
                          + "excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "8d6cc5cf-c973-11eb-aaaa-000000000041", "1623278362400041",
                    "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithAllWorkRequestContextScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
            new SearchParameterRequestContext(REQUEST_CONTEXT, SearchOperator.BOOLEAN, RequestContext.ALL_WORK)
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("all_work should return only unassigned and MANAGE permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("all_work should return only unassigned and MANAGE permission and PRIVATE")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("all_work should return only unassigned and MANAGE permission and "
                          + "excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(granularPermissionUserInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> sortByFieldScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario sortByCaseId = TaskQueryScenario.builder()
            .scenarioName("Sort by caseId")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042"
                )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(
                new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)
            )
        );

        final TaskQueryScenario sortByMultipleFields = TaskQueryScenario.builder()
            .scenarioName("Sort by caseName and caseId")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004"
                )
            ).build();

        return Stream.of(
            sortByCaseId,
            sortByMultipleFields
        );
    }

    private static Stream<TaskQueryScenario> sortByFieldNexHearingDateAscOrder() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario sortByNextHearingDateAsc = TaskQueryScenario.builder()
            .scenarioName("Sort by next hearing date asc")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "2022-10-09T20:09:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "2022-10-09T20:10:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "2022-10-09T20:11:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "2022-10-09T20:12:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "2022-10-09T20:13:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "2022-10-09T20:14:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "2022-10-09T20:15:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "2022-10-09T20:16:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "2022-10-09T20:16:45.345"
                )
            ).build();

        return Stream.of(sortByNextHearingDateAsc);
    }


    private static Stream<TaskQueryScenario> sortByFieldNexHearingDateDescOrder() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario sortByNextHearingDateDesc = TaskQueryScenario.builder()
            .scenarioName("Sort by next hearing date desc")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(9)
            .expectedTotalRecords(9)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "2022-10-09T20:16:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000042", "1623278362400042",
                    "2022-10-09T20:16:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "2022-10-09T20:15:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "2022-10-09T20:14:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "2022-10-09T20:13:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "2022-10-09T20:12:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "2022-10-09T20:11:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "2022-10-09T20:10:45.345",
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "2022-10-09T20:09:45.345"
                )
            ).build();

        return Stream.of(sortByNextHearingDateDesc);
    }

    private static Stream<TaskQueryScenario> paginatedResultsScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION)),
                new SearchParameterList(WORK_TYPE, IN, List.of("hearing_work", "access_requests"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("All records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(20)
            .expectedTotalRecords(25)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000021", "1623278362400021",
                    "8d6cc5cf-c973-11eb-aaaa-000000000022", "1623278362400022",
                    "8d6cc5cf-c973-11eb-aaaa-000000000023", "1623278362400023",
                    "8d6cc5cf-c973-11eb-aaaa-000000000024", "1623278362400024",
                    "8d6cc5cf-c973-11eb-aaaa-000000000025", "1623278362400025",
                    "8d6cc5cf-c973-11eb-aaaa-000000000028", "1623278362400028",
                    "8d6cc5cf-c973-11eb-aaaa-000000000029", "1623278362400029",
                    "8d6cc5cf-c973-11eb-aaaa-000000000030", "1623278362400030"
                )
            ).build();

        final TaskQueryScenario firstPage = TaskQueryScenario.builder()
            .scenarioName("First ten records")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(10)
            .expectedTotalRecords(25)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005",
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010"
                )
            ).build();

        final TaskQueryScenario firstTwoRecords = TaskQueryScenario.builder()
            .scenarioName("First two records")
            .firstResult(0)
            .maxResults(2)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(25)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002"
                )
            ).build();

        final TaskQueryScenario secondPage = TaskQueryScenario.builder()
            .scenarioName("Should start on page 2")
            .firstResult(5)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(10)
            .expectedTotalRecords(25)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000006", "1623278362400006",
                    "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                    "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                    "8d6cc5cf-c973-11eb-aaaa-000000000009", "1623278362400009",
                    "8d6cc5cf-c973-11eb-aaaa-000000000010", "1623278362400010",
                    "8d6cc5cf-c973-11eb-aaaa-000000000011", "1623278362400011",
                    "8d6cc5cf-c973-11eb-aaaa-000000000012", "1623278362400012",
                    "8d6cc5cf-c973-11eb-aaaa-000000000021", "1623278362400021",
                    "8d6cc5cf-c973-11eb-aaaa-000000000022", "1623278362400022",
                    "8d6cc5cf-c973-11eb-aaaa-000000000023", "1623278362400023"
                )
            ).build();

        final TaskQueryScenario twoPages = TaskQueryScenario.builder()
            .scenarioName("Should have 5 pages")
            .firstResult(0)
            .maxResults(5)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(5)
            .expectedTotalRecords(25)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                    "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                    "8d6cc5cf-c973-11eb-aaaa-000000000003", "1623278362400003",
                    "8d6cc5cf-c973-11eb-aaaa-000000000004", "1623278362400004",
                    "8d6cc5cf-c973-11eb-aaaa-000000000005", "1623278362400005"
                )
            ).build();

        return Stream.of(
            allTasks,
            firstPage,
            firstTwoRecords,
            secondPage,
            twoPages
        );
    }

    private static Stream<TaskQueryScenario> inActiveRole() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.INACTIVE_ROLE)
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final TaskQueryScenario inActive = TaskQueryScenario.builder()
            .scenarioName("inactive_role")
            .firstResult(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedAmountOfTasksInResponse(0)
            .expectedTaskDetails(emptyList())
            .userInfo(userInfo)
            .build();

        return Stream.of(
            inActive
        );
    }

    private static Stream<TaskQueryScenario> inValidBeginAndEndTime() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_RESTRICTED)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .build()
            )
            .beginTime(null)
            .endTime(null)
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final TaskQueryScenario invalidBeginAndEndTime = TaskQueryScenario.builder()
            .scenarioName("invalid-begin-end-time")
            .firstResult(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedAmountOfTasksInResponse(9)
            .userInfo(userInfo)
            .build();

        return Stream.of(
            invalidBeginAndEndTime
        );
    }

    private static List<RoleAssignment> invalidCaseRoleAssignmentsWithGrantTypeSpecific() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_LEAD_JUDGE_RESTRICTED)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .baseLocation(PRIMARY_LOCATION)
                    .caseId("1623278362400001")
                    .build()
            )
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeSpecific(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("SPECIFIC_LEAD_JUDGE_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(SSCS_JURISDICTION)
                    .caseId("1623278362400013")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("SPECIFIC_LEAD_JUDGE_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId("1623278362400013")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeStandard(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + classification.name())
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
                TestRolesWithGrantType.valueOf("STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_" + classification.name())
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

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeChallenged(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("CHALLENGED_ACCESS_JUDICIARY_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(SSCS_JURISDICTION)
                    .build()
            )
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("CHALLENGED_ACCESS_JUDICIARY_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .build()
            )
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeStandardAndExcluded(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + classification.name())
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

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId("1623278362431003")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeChallengedAndExcluded(
        Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("CHALLENGED_ACCESS_JUDICIARY_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .build()
            )
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseId("1623278362400016")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> sortByField(Classification classification) {

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + classification)
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .region("1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static List<RoleAssignment> pagination(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_TRIBUNAL_CASE_WORKER_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(
                TestRolesWithGrantType.valueOf("PAGINATION_ROLE_" + classification.name())
            )
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .baseLocation(PRIMARY_LOCATION)
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        return roleAssignments;
    }

    private static SearchTaskRequest invalidJurisdiction() {
        return new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA1", "", null))
        ));
    }

    private static SearchTaskRequest invalidLocation() {
        return new SearchTaskRequest(List.of(
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("000000", "", null))
        ));
    }

    private static SearchTaskRequest invalidCaseId() {
        return new SearchTaskRequest(List.of(
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList("000000", "", null))
        ));
    }

    private static SearchTaskRequest invalidUserId() {
        return new SearchTaskRequest(List.of(
            new SearchParameterList(USER, SearchOperator.IN, asList("unknown", "", null))
        ));
    }

    private static Stream<TaskQueryScenario> searchByJurisdictionAndLocationScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by jurisdiction and location")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(5)
            .expectedTotalRecords(5)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000002", "1623278362400002",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000007", "1623278362400007",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000008", "1623278362400008",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000040", "1623278362400040"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("INVALID"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyJurisdictionTasks = TaskQueryScenario.builder()
            .scenarioName("Search by invalid jurisdiction, empty results")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("INVALID"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyLocationTasks = TaskQueryScenario.builder()
            .scenarioName("Search by invalid location, empty results")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(allTasks, emptyJurisdictionTasks, emptyLocationTasks);
    }

    private static Stream<TaskQueryScenario> searchByStateScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(STATE, SearchOperator.IN, List.of("PENDING_AUTO_ASSIGN"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by state")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000032", "1623278362400032"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(STATE, SearchOperator.IN, List.of("CANCELLED"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyTasks = TaskQueryScenario.builder()
            .scenarioName("Search by state, empty results")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(allTasks, emptyTasks);
    }

    private static Stream<TaskQueryScenario> searchByRoleCategoryScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, List.of("CTSC"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by role category")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000029", "1623278362400029"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, List.of("CTSC", "ADMIN"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario multipleTasks = TaskQueryScenario.builder()
            .scenarioName("Search by role category, multiple")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000029", "1623278362400029",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000030", "1623278362400030"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, List.of("LEGAL_OPERATIONS"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyTasks = TaskQueryScenario.builder()
            .scenarioName("Search by role category, empty results")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(allTasks, multipleTasks, emptyTasks);
    }

    private static Stream<TaskQueryScenario> searchByCaseIdScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362400001"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by case id")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000001", "1623278362400001"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("0000000000000000"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyTasks = TaskQueryScenario.builder()
            .scenarioName("Search by case id, empty results")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(allTasks, emptyTasks);
    }

    private static Stream<TaskQueryScenario> searchByJurisdictionLocationAndStateScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION)),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("COMPLETED", "PENDING_AUTO_ASSIGN"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by jurisdiction, location and state")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000031", "1623278362400031",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000032", "1623278362400032"
                                 )
            ).build();

        return Stream.of(allTasks);
    }


    private static Stream<TaskQueryScenario> searchByWorkTypeScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION)),
                new SearchParameterList(WORK_TYPE, IN, singletonList("review_case"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Search by work type")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-aaaa-000000000035", "1623278362400035"
                )
            ).build();


        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION)),
                new SearchParameterList(WORK_TYPE, IN, List.of("review_case", "access_requests"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario multipleWorkType = TaskQueryScenario.builder()
            .scenarioName("Search by work type, multiple")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                                     "8d6cc5cf-c973-11eb-aaaa-000000000032", "1623278362400032",
                                     "8d6cc5cf-c973-11eb-aaaa-000000000035", "1623278362400035"
                                 )
            ).build();

        searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(WA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of(PRIMARY_LOCATION)),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362400035")),
                new SearchParameterList(WORK_TYPE, IN, singletonList("routine_work"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario emptyWorkType = TaskQueryScenario.builder()
            .scenarioName("Search by work type, empty")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .userInfo(userInfo)
            .build();

        return Stream.of(allTasks, multipleWorkType, emptyWorkType);
    }

    @Builder
    private static class TaskQueryScenario {
        public UserInfo userInfo;
        String scenarioName;
        int firstResult;
        int maxResults;
        List<RoleAssignment> roleAssignments;
        SearchTaskRequest searchTaskRequest;
        int expectedAmountOfTasksInResponse;
        int expectedTotalRecords;
        List<String> expectedTaskDetails;

        @Override
        public String toString() {
            return this.scenarioName;
        }

    }
}
