package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import com.launchdarkly.shaded.com.google.common.collect.Lists;
import lombok.Builder;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wataskmanagementapi.CftRepositoryBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;


@Sql("/scripts/data.sql")
public class CftQueryServiceTest extends CftRepositoryBaseTest {

    private List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        cftQueryService = new CftQueryService(taskResourceRepository);

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeBasicScenarioProviderHappyPath",
        "grantTypeSpecificScenarioProviderHappyPath",
        "grantTypeStandardAndExcludedScenarioProviderHappyPath",
        "grantTypeChallengedAndExcludedScenarioProviderHappyPath",
        "sortByFieldScenario",
        "paginatedResultsScenario"
    })
    void shouldRetrieveTasks(TaskQuerycenario scenario) {
        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final List<TaskResource> allTasks = cftQueryService.getAllTasks(
            scenario.firstResults, scenario.maxResults, scenario.searchTaskRequest,
            accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks)
            .hasSize(scenario.expectedSize)
            .flatExtracting(TaskResource::getTaskId, TaskResource::getCaseId)
            .containsExactly(
                scenario.expectedTaskDetails.toArray()
            );
    }

    @ParameterizedTest
    @MethodSource("invalidExUiSearchQuery")
    void shouldReturnEmptyTasksWithInvalidExUiSearchQuery(TaskQuerycenario scenario) {
        //given
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final List<TaskResource> allTasks = cftQueryService.getAllTasks(
            1, 10, scenario.searchTaskRequest, accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks)
            .isEmpty();
    }

    @Builder
    private static class TaskQuerycenario {
        String scenarioName;
        int firstResults;
        int maxResults;
        List<RoleAssignment> roleAssignments;
        SearchTaskRequest searchTaskRequest;
        int expectedSize;
        List<String> expectedTaskDetails;

        @Override
        public String toString() {
            return "TaskQuerycenario{"
                   + "scenarioName='" + scenarioName + '\''
                   + '}';
        }
    }

    private static Stream<TaskQuerycenario> grantTypeBasicScenarioProviderHappyPath() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQuerycenario publicClassification = TaskQuerycenario.builder()
            .scenarioName("basic_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PUBLIC))
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007"
                )
            ).build();

        final TaskQuerycenario privateClassification = TaskQuerycenario.builder()
            .scenarioName("basic_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PRIVATE))
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007",
                "8d6cc5cf-c973-11eb-bdba-0242ac111008", "1623278362431008"
                )
            ).build();

        final TaskQuerycenario restrictedClassification = TaskQuerycenario.builder()
            .scenarioName("basic_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.RESTRICTED))
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007",
                "8d6cc5cf-c973-11eb-bdba-0242ac111008", "1623278362431008",
                "8d6cc5cf-c973-11eb-bdba-0242ac111009", "1623278362431009"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
           restrictedClassification
        );
    }

    private static Stream<TaskQuerycenario> grantTypeSpecificScenarioProviderHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ));

        final TaskQuerycenario publicClassification = TaskQuerycenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQuerycenario privateClassification = TaskQuerycenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQuerycenario restrictedClassification = TaskQuerycenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQuerycenario> grantTypeStandardAndExcludedScenarioProviderHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324")),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
        ));

        final TaskQuerycenario publicClassification = TaskQuerycenario.builder()
            .scenarioName("standard_with_excluded_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQuerycenario privateClassification = TaskQuerycenario.builder()
            .scenarioName("standard_with_excluded_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQuerycenario restrictedClassification = TaskQuerycenario.builder()
            .scenarioName("standard_with_excluded_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQuerycenario> grantTypeChallengedAndExcludedScenarioProviderHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQuerycenario publicClassification = TaskQuerycenario.builder()
            .scenarioName("challenged_with_excluded_grant_type_with_authorizations_and_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
                )
            ).build();

        final TaskQuerycenario privateClassification = TaskQuerycenario.builder()
            .scenarioName("challenged_with_excluded_grant_type_with_authorizations_and_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
                )
            ).build();

        final TaskQuerycenario restrictedClassification = TaskQuerycenario.builder()
            .scenarioName("challenged_with_excluded_grant_type_with_authorizations_and_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQuerycenario> sortByFieldScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQuerycenario sortByCaseId = TaskQuerycenario.builder()
            .scenarioName("Sort by caseId")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002"
                )
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(
            new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
            new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)
        )
        );

        final TaskQuerycenario sortByMultipleFields = TaskQuerycenario.builder()
            .scenarioName("Sort by caseName and caseId")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001"
                )
            ).build();

        return Stream.of(
            sortByCaseId,
            sortByMultipleFields
        );
    }

    private static Stream<TaskQuerycenario> paginatedResultsScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQuerycenario allTasks = TaskQuerycenario.builder()
            .scenarioName("First ten records")
            .firstResults(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(11)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111010", "1623278362431010",
                "8d6cc5cf-c973-11eb-bdba-0242ac111011", "1623278362431011",
                "8d6cc5cf-c973-11eb-bdba-0242ac111012", "1623278362431012",
                "8d6cc5cf-c973-11eb-bdba-0242ac111013", "1623278362431013",
                "8d6cc5cf-c973-11eb-bdba-0242ac111014", "1623278362431014"
                )
            ).build();

        final TaskQuerycenario firstPage = TaskQuerycenario.builder()
            .scenarioName("First ten records")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(10)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111010", "1623278362431010",
                "8d6cc5cf-c973-11eb-bdba-0242ac111011", "1623278362431011",
                "8d6cc5cf-c973-11eb-bdba-0242ac111012", "1623278362431012",
                "8d6cc5cf-c973-11eb-bdba-0242ac111013", "1623278362431013"
                )
            ).build();

        final TaskQuerycenario firstTwoRecords = TaskQuerycenario.builder()
            .scenarioName("First two records")
            .firstResults(0)
            .maxResults(2)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001"
                )
            ).build();

        final TaskQuerycenario secondPage = TaskQuerycenario.builder()
            .scenarioName("Starting with third record")
            .firstResults(1)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111014", "1623278362431014"
                )
            ).build();

        return Stream.of(
            allTasks,
            firstPage,
            firstTwoRecords,
            secondPage
        );
    }

    private static Stream<TaskQuerycenario> invalidExUiSearchQuery() {
        final TaskQuerycenario invalidJurisdiction = TaskQuerycenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(getSearchTaskRequest("IA1", null, "ASSIGNED", null))
            .build();

        final TaskQuerycenario invalidLocation = TaskQuerycenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(getSearchTaskRequest("IA", "12345", "ASSIGNED", ""))
            .build();

        final TaskQuerycenario invalidUser = TaskQuerycenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(getSearchTaskRequest(null, null, "ASSIGNED", "user1"))
            .build();

        return Stream.of(invalidJurisdiction, invalidLocation, invalidUser);
    }

    @NotNull
    private static SearchTaskRequest getSearchTaskRequest(
        String jurisdiction, String location, String state, String assignee
    ) {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList(jurisdiction)),
            new SearchParameter(LOCATION, SearchOperator.IN, asList(location)),
            new SearchParameter(STATE, SearchOperator.IN, asList(state)),
            new SearchParameter(USER, SearchOperator.IN, asList(assignee)),
            new SearchParameter(CASE_ID, SearchOperator.IN, asList(assignee))
        ));
        return searchTaskRequest;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeBasic(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("other-caseworker")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeSpecific(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.SPECIFIC)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.SPECIFIC)
            .attributes(stcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeStandard(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS",
            RoleAttributeDefinition.BASE_LOCATION.value(), "653255",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431005"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431005"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeChallenged(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003",
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003",
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> sortByField(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431005"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> pagination(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("pagination-role")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> mapRoleAssignments(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> attributes = Map.of(RoleAttributeDefinition.CASE_TYPE.value(), "Asylum");
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .attributes(attributes)
            .build();
        roleAssignments.add(roleAssignment);
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .attributes(attributes)
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

}
