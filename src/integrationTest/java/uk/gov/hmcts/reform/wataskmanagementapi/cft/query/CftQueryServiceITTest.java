package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.shaded.com.google.common.collect.Lists;
import lombok.Builder;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/data.sql")
public class CftQueryServiceITTest {

    private List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, taskResourceRepository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "grantTypeBasicScenarioHappyPath",
        "grantTypeSpecificScenarioHappyPath",
        "grantTypeStandardScenarioHappyPath",
        "grantTypeChallengedScenarioHappyPath",
        "grantTypeWithStandardAndExcludedScenarioHappyPath",
        "grantTypeWithChallengedAndExcludedScenarioHappyPath",
        "grantTypeWithAvailableTasksOnlyScenarioHappyPath",
        "withAllGrantTypesHappyPath",
        "inActiveRole",
        "sortByFieldScenario",
        "paginatedResultsScenario"
    })
    void shouldRetrieveTasks(TaskQueryScenario scenario) {
        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResults, scenario.maxResults, scenario.searchTaskRequest,
            accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedSize)
            .flatExtracting(Task::getId, Task::getCaseId)
            .containsExactly(
                scenario.expectedTaskDetails.toArray()
            );
    }

    @ParameterizedTest
    @MethodSource({
         "grantTypeBasicErrorScenario",
        "grantTypeSpecificErrorScenario",
        "grantTypeStandardErrorScenario",
        "grantTypeChallengedErrorScenario",
        "grantTypeWithStandardAndExcludedErrorScenario",
        "grantTypeWithChallengedAndExcludedErrorScenario",
        "inValidBeginAndEndTime"
    })
    void shouldReturnEmptyTasksWithInvalidExUiSearchQuery(TaskQueryScenario scenario) {
        //given
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResults, scenario.maxResults, scenario.searchTaskRequest,
            accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedSize);
    }

    @Test
    void handlePaginationError() {
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null,
            List.of(RoleAssignment.builder().build()));
        permissionsRequired.add(PermissionTypes.READ);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            -1, 1, searchTaskRequest, accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks.getTasks())
            .isEmpty();

        allTasks = cftQueryService.searchForTasks(
            0, 0, searchTaskRequest, accessControlResponse, permissionsRequired
        );
        //then
        Assertions.assertThat(allTasks.getTasks())
            .isEmpty();

        allTasks = cftQueryService.searchForTasks(
            1, -1, searchTaskRequest, accessControlResponse, permissionsRequired
        );
        //then
        Assertions.assertThat(allTasks.getTasks())
            .isEmpty();

        allTasks = cftQueryService.searchForTasks(
            -1, -1, searchTaskRequest, accessControlResponse, permissionsRequired
        );
        //then
        Assertions.assertThat(allTasks.getTasks())
            .isEmpty();
    }

    @Builder
    private static class TaskQueryScenario {
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

    private static Stream<TaskQueryScenario> grantTypeBasicScenarioHappyPath() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PUBLIC))
            .expectedSize(1)
            // taskId and caseId
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
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

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
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

    private static Stream<TaskQueryScenario> grantTypeBasicErrorScenario() {

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("other-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PRIVATE))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PRIVATE))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PRIVATE))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("basic_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeBasic(Classification.PRIVATE))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList(
                "1623278362431003"
            ))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
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

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
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

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
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

    private static Stream<TaskQueryScenario> grantTypeSpecificErrorScenario() {

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(GrantType.SPECIFIC)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        return Stream.of(
            withAuthorizations,
            invalidJurisdiction,
            invalidLocation,
            invalidCaseId,
            invalidUser
        );
    }

    private static Stream<TaskQueryScenario> grantTypeStandardScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("standard-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .authorisations(List.of("unknownValue"))
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006"
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
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();


        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();


        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        return Stream.of(
            invalidJurisdiction,
            invalidLocation,
            invalidCaseId,
            invalidUser
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithStandardAndExcludedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("standard-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .authorisations(List.of("unknownValue"))
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(stcAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidAuthorization = TaskQueryScenario.builder()
            .scenarioName("standard_and_excluded_grant_type_with_invalid_authorization")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        return Stream.of(
            invalidAuthorization
        );

    }

    private static Stream<TaskQueryScenario> grantTypeWithChallengedAndExcludedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_private")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000"
                )
            ).build();

        return Stream.of(
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> grantTypeWithAvailableTasksOnlyScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(1)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(3)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003"
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(tcAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(stcAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_and_excluded_grant_type_with_invalid_caseId")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResults(0)
            .maxResults(10)
            .expectedSize(0)
            .expectedTaskDetails(Collections.emptyList()).build();

        return Stream.of(
            invalidCaseId
        );

    }

    private static Stream<TaskQueryScenario> withAllGrantTypesHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("includes_all_grant_types_with_classification_as_restricted")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentWithAllGrantTypes(Classification.RESTRICTED))
            .expectedSize(7)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006",
                "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007",
                "8d6cc5cf-c973-11eb-bdba-0242ac111008", "1623278362431008",
                "8d6cc5cf-c973-11eb-bdba-0242ac111009", "1623278362431009"
                )
            ).build();

        return Stream.of(
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> sortByFieldScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQueryScenario sortByCaseId = TaskQueryScenario.builder()
            .scenarioName("Sort by caseId")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005"
                )
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(
            new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT),
            new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)
        )
        );

        final TaskQueryScenario sortByMultipleFields = TaskQueryScenario.builder()
            .scenarioName("Sort by caseName and caseId")
            .firstResults(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001"
                )
            ).build();

        return Stream.of(
            sortByCaseId,
            sortByMultipleFields
        );
    }

    private static Stream<TaskQueryScenario> paginatedResultsScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("All records")
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

        final TaskQueryScenario firstPage = TaskQueryScenario.builder()
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

        final TaskQueryScenario firstTwoRecords = TaskQueryScenario.builder()
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

        final TaskQueryScenario secondPage = TaskQueryScenario.builder()
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

    private static Stream<TaskQueryScenario> inActiveRole() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("inActiveRole")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario inActive = TaskQueryScenario.builder()
            .scenarioName("inactive_role")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedSize(0)
            .expectedTaskDetails(Lists.newArrayList()
            ).build();

        return Stream.of(
            inActive
        );
    }

    private static Stream<TaskQueryScenario> inValidBeginAndEndTime() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .beginTime(null)
            .endTime(null)
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidBeginAndEndTime = TaskQueryScenario.builder()
            .scenarioName("invalid-begin-end-time")
            .firstResults(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList()
            ).build();

        return Stream.of(
            invalidBeginAndEndTime
        );
    }

    @NotNull
    private static SearchTaskRequest getSearchTaskRequest(
        String jurisdiction, String location, String state, String assignee
    ) {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList(jurisdiction)),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList(location)),
            new SearchParameterList(STATE, SearchOperator.IN, asList(state)),
            new SearchParameterList(USER, SearchOperator.IN, asList(assignee)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, asList(assignee))
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
            RoleAttributeDefinition.BASE_LOCATION.value(), "653255"
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
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
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
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeStandardAndExcluded(
        Classification classification
    ) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
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
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeChallengedAndExcluded(
        Classification classification
    ) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
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
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431006"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentWithAllGrantTypes(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(specificAttributes)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stdAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> excludeddAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(excludeddAttributes)
            .grantType(GrantType.EXCLUDED)
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
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
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
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .attributes(attributes)
            .build();
        roleAssignments.add(roleAssignment);
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .attributes(attributes)
            .build();
        roleAssignments.add(roleAssignment);

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
}
