package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static com.launchdarkly.shaded.com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/ia/data.sql")
@Slf4j
public class CftQueryServiceITTest extends RoleAssignmentHelper {

    private final List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @MockBean
    private CamundaService camundaService;

    private CftQueryService cftQueryService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
    }

    @ParameterizedTest()
    @MethodSource({
        "grantTypeSpecificScenarioHappyPath",
        "grantTypeStandardScenarioHappyPath",
        "grantTypeChallengedScenarioHappyPath",
        "grantTypeWithStandardAndExcludedScenarioHappyPath",
        "grantTypeWithChallengedAndExcludedScenarioHappyPath",
        "grantTypeWithAvailableTasksOnlyScenarioHappyPath",
        "withAllGrantTypesHappyPath",
        "inActiveRole",
        "sortByFieldScenario",
        "paginatedResultsScenario",
        "defaultSortedResultsScenario",
        "secondaryLocationSortedResultsAscScenario",
        "secondaryLocationSortedResultsDscScenario",
        "secondaryCaseNameSortedResultsAscScenario",
        "secondaryCaseNameSortedResultsDescScenario",
        "secondaryDueDateSortedResultsAscScenario",
        "secondaryCaseIdSortedResultsDescScenario",
        "secondaryTaskTitleSortedResultsAscScenario",
        "secondaryTaskTitleSortedResultsDescScenario",
        "secondaryCaseCategorySortedResultsAscScenario",
        "secondaryCaseCategorySortedResultsDescScenario"
    })
    void shouldRetrieveTasks(TaskQueryScenario scenario) {
        log.info("Running scenario: {}", scenario.scenarioName);

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final GetTasksResponse<Task> allTasks = cftQueryService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            scenario.searchTaskRequest,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks.getTotalRecords())
            .isEqualTo(scenario.expectedTotalRecords);
        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedAmountOfTasksInResponse)
            .flatExtracting(Task::getId, Task::getCaseId, Task::getCaseName, Task::getLocationName,
                            Task::getTaskTitle, Task::getCaseCategory)
            .containsExactly(
                scenario.expectedTaskDetails.toArray()
            );

        //then
        if (scenario.expectedDueDates != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss");

            List<String> expectedDates = scenario.expectedDueDates.stream().map(t -> t.format(formatter)).collect(
                Collectors.toList());

            List<String> actualDates = allTasks.getTasks().stream().map(t -> t.getDueDate().format(formatter)).collect(
                Collectors.toList());

            Assertions.assertThat(actualDates.toArray()).contains(expectedDates.toArray());
        }
    }

    @ParameterizedTest
    @MethodSource({
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
            scenario.firstResult, scenario.maxResults, scenario.searchTaskRequest,
            accessControlResponse.getRoleAssignments(), permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks.getTasks())
            .hasSize(scenario.expectedAmountOfTasksInResponse);
    }

    @Test
    void handlePaginationError() {
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            List.of(RoleAssignment.builder().build())
        );
        permissionsRequired.add(PermissionTypes.READ);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        Assertions.assertThatThrownBy(() -> cftQueryService.searchForTasks(
                -1, 1, searchTaskRequest, accessControlResponse.getRoleAssignments(), permissionsRequired
            ))
            .hasNoCause()
            .hasMessage("Offset index must not be less than zero");


        Assertions.assertThatThrownBy(() -> cftQueryService.searchForTasks(
                0, 0, searchTaskRequest, accessControlResponse.getRoleAssignments(), permissionsRequired
            ))
            .hasNoCause()
            .hasMessage("Limit must not be less than one");
    }

    private static Stream<TaskQueryScenario> grantTypeSpecificScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, List.of(
                "1623278362431003"
            ))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .expectedTaskDetails(newArrayList(
                        "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                        "TestCase2", "Taylor House", "title", null
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
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null
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
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null
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
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .endTime(LocalDateTime.now().plusYears(1))
            .roleType(RoleType.CASE)
            .grantType(GrantType.SPECIFIC)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("specific_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeSpecific(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(4)
            .expectedTotalRecords(4)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(6)
            .expectedTotalRecords(6)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("standard-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .roleType(RoleType.ORGANISATION)
            .authorisations(List.of("unknownValue"))
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario withAuthorizations = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_authorizations")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidJurisdiction = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_jurisdiction")
            .searchTaskRequest(invalidJurisdiction())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("standard_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(4)
            .expectedTotalRecords(4)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111026", "1623278362431026",
                    "TestCase", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(6)
            .expectedTotalRecords(6)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111026", "1623278362431026",
                    "TestCase", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111026", "1623278362431026",
                    "TestCase", "Taylor House", "title", null
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
            .build();


        final TaskQueryScenario invalidLocation = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_location")
            .searchTaskRequest(invalidLocation())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();


        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_caseId")
            .searchTaskRequest(invalidCaseId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        final TaskQueryScenario invalidUser = TaskQueryScenario.builder()
            .scenarioName("challenged_grant_type_with_invalid_user")
            .searchTaskRequest(invalidUserId())
            .roleAssignments(roleAssignmentsWithGrantTypeChallenged(Classification.RESTRICTED))
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ), List.of(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT)));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null
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
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("standard-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .roleType(RoleType.ORGANISATION)
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
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidAuthorization = TaskQueryScenario.builder()
            .scenarioName("standard_and_excluded_grant_type_with_invalid_authorization")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        return Stream.of(
            invalidAuthorization
        );

    }

    private static Stream<TaskQueryScenario> grantTypeWithChallengedAndExcludedScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_public")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111026", "1623278362431026",
                    "TestCase", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_private")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeChallengedAndExcluded(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(5)
            .expectedTotalRecords(5)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111026", "1623278362431026",
                    "TestCase", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandardAndExcluded(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks_only should return only unassigned and OWN permission and PRIVATE")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
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
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
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
            .roleType(RoleType.CASE)
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
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidCaseId = TaskQueryScenario.builder()
            .scenarioName("challenged_and_excluded_grant_type_with_invalid_caseId")
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .firstResult(0)
            .maxResults(10)
            .expectedAmountOfTasksInResponse(0)
            .expectedTotalRecords(0)
            .build();

        return Stream.of(
            invalidCaseId
        );

    }

    private static Stream<TaskQueryScenario> withAllGrantTypesHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ), List.of(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT)));

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("includes_all_grant_types_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignmentWithAllGrantTypes(Classification.RESTRICTED))
            .expectedAmountOfTasksInResponse(10)
            .expectedTotalRecords(14)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005", "TestCase3",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025", "TestCase3",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004", "TestCase2",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024", "TestCase2",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003", "TestCase2",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023", "TestCase2",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111007", "1623278362431007", "TestCase4",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111008", "1623278362431008", "TestCase4",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111009", "1623278362431009", "TestCase4",
                    "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111006", "1623278362431006", "TestCase",
                    "Taylor House", "title", null
            ))
            .expectedDueDates(newArrayList(ZonedDateTime.parse("2022-10-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-10-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-09-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-09-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-08-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-08-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                                           ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00")
            ))
            .build();

        return Stream.of(
            restrictedClassification
        );
    }

    private static Stream<TaskQueryScenario> sortByFieldScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQueryScenario sortByCaseId = TaskQueryScenario.builder()
            .scenarioName("Sort by caseId")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(sortByField(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null
                )
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ), List.of(
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
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null
                )
            ).build();

        return Stream.of(
            sortByCaseId,
            sortByMultipleFields
        );
    }

    private static Stream<TaskQueryScenario> paginatedResultsScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324"))
        ), List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("All records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(20)
            .expectedTotalRecords(22)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111010", "1623278362431010",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111011", "1623278362431011",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111012", "1623278362431012",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111013", "1623278362431013",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111014", "1623278362431014",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111030", "1623278362431030",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111031", "1623278362431031",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111032", "1623278362431032",
                    "TestCase4", "Taylor House", "title", null

                )
            ).build();

        final TaskQueryScenario firstPage = TaskQueryScenario.builder()
            .scenarioName("First ten records")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(10)
            .expectedTotalRecords(22)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111010", "1623278362431010",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111011", "1623278362431011",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111012", "1623278362431012",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111013", "1623278362431013",
                    "TestCase4", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario firstTwoRecords = TaskQueryScenario.builder()
            .scenarioName("First two records")
            .firstResult(0)
            .maxResults(2)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(22)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000", "TestCase4", "Taylor House", "title", null,
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001", "TestCase", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario secondPage = TaskQueryScenario.builder()
            .scenarioName("Should start on page 2")
            .firstResult(5)
            .maxResults(10)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(10)
            .expectedTotalRecords(22)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111010", "1623278362431010",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111011", "1623278362431011",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111012", "1623278362431012",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111013", "1623278362431013",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111014", "1623278362431014",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111020", "1623278362431020",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111021", "1623278362431021",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111022", "1623278362431022",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario twoPages = TaskQueryScenario.builder()
            .scenarioName("Should have 5 pages")
            .firstResult(0)
            .maxResults(5)
            .roleAssignments(pagination(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(5)
            .expectedTotalRecords(22)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "TestCase4", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                    "TestCase", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                    "TestCase6", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null
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

    private static Stream<TaskQueryScenario> defaultSortedResultsScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                                 "TestCase4", "Taylor House", "title", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                                 "TestCase4", "Taylor House", "title", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                                 "TestCase4", "Taylor House", "title", "appealType",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                                 "TestCase4", "Taylor House", "title", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                                 "TestCase4", "Taylor House", "aaa", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                                 "TestCase1", "Cardiff Crown Court", "title", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                                 "TestCase4", "Taylor House", "title", "Protection",
                                 "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                                 "TestCase4", "Taylor House", "title", "Protection"
                                 )
            ).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryLocationSortedResultsAscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.LOCATION_NAME_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary ascending sort on location with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005", "TestCase1", "Cardiff Crown Court",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002", "TestCase4", "Taylor House",
                "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004", "TestCase4", "Taylor House",
                "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007", "TestCase4", "Taylor House",
                "title", "Protection")
            ).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryCaseNameSortedResultsAscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary ascending sort on case name with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005", "TestCase1", "Cardiff Crown Court",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002", "TestCase4", "Taylor House",
                "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004", "TestCase4", "Taylor House",
                "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007", "TestCase4", "Taylor House",
                "title", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryCaseNameSortedResultsDescScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.CASE_NAME_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary descending sort on case name with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002", "TestCase4", "Taylor House",
                "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004", "TestCase4", "Taylor House",
                "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007", "TestCase4", "Taylor House",
                "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005", "TestCase1", "Cardiff Crown Court",
                "title", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryDueDateSortedResultsAscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary ascending sort on due date with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007", "TestCase4",
                "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000", "TestCase4",
                "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001", "TestCase4",
                "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002", "TestCase4",
                "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003", "TestCase4",
                "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004", "TestCase4",
                "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005", "TestCase1",
                "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006", "TestCase4",
                "Taylor House", "title", "Protection"
            ))
            .expectedDueDates(newArrayList(ZonedDateTime.parse("2022-05-08T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                              ZonedDateTime.parse("2022-05-09T20:15:45.345875+01:00")))
            .build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryCaseIdSortedResultsDescScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary descending sort on case id with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryTaskTitleSortedResultsAscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.TASK_TITLE_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary ascending sort on task title with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryTaskTitleSortedResultsDescScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.TASK_TITLE_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary descending sort on task title with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryCaseCategorySortedResultsAscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary ascending sort on case category with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection"
            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryCaseCategorySortedResultsDescScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary descending sort on case category with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType"

            )).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> secondaryLocationSortedResultsDscScenario() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))),
            List.of(new SortingParameter(SortField.LOCATION_NAME_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        final TaskQueryScenario allTasks = TaskQueryScenario.builder()
            .scenarioName("Secondary descending sort on location with default sort all records")
            .firstResult(0)
            .maxResults(20)
            .roleAssignments(defaultSort(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(8)
            .expectedTotalRecords(8)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac222000", "1623278362222000",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222001", "1623278362222001",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222002", "1623278362222002",
                "TestCase4", "Taylor House", "title", "appealType",
                "8d6cc5cf-c973-11eb-bdba-0242ac222003", "1623278362222003",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222004", "1623278362222004",
                "TestCase4", "Taylor House", "aaa", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222006", "1623278362222006",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222007", "1623278362222007",
                "TestCase4", "Taylor House", "title", "Protection",
                "8d6cc5cf-c973-11eb-bdba-0242ac222005", "1623278362222005",
                "TestCase1", "Cardiff Crown Court", "title", "Protection")
            ).build();

        return Stream.of(
            allTasks
        );
    }

    private static Stream<TaskQueryScenario> inActiveRole() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("inActiveRole")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario inActive = TaskQueryScenario.builder()
            .scenarioName("inactive_role")
            .firstResult(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedAmountOfTasksInResponse(0)
            .expectedTaskDetails(emptyList())
            .build();

        return Stream.of(
            inActive
        );
    }

    private static Stream<TaskQueryScenario> inValidBeginAndEndTime() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(null)
            .endTime(null)
            .build();
        roleAssignments.add(roleAssignment);

        final TaskQueryScenario invalidBeginAndEndTime = TaskQueryScenario.builder()
            .scenarioName("invalid-begin-end-time")
            .firstResult(0)
            .maxResults(10)
            .searchTaskRequest(searchTaskRequest)
            .roleAssignments(roleAssignments)
            .expectedAmountOfTasksInResponse(8)
            .build();

        return Stream.of(
            invalidBeginAndEndTime
        );
    }

    @NotNull
    private static SearchTaskRequest getSearchTaskRequest(
        String jurisdiction, String location, String state, String assignee
    ) {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(jurisdiction)),
            new SearchParameterList(LOCATION, SearchOperator.IN, List.of(location)),
            new SearchParameterList(STATE, SearchOperator.IN, List.of(state)),
            new SearchParameterList(USER, SearchOperator.IN, List.of(assignee)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, List.of(assignee))
        ));
        return searchTaskRequest;
    }

    private static List<RoleAssignment> roleAssignmentsWithGrantTypeSpecific(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> tcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), "SCSS",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .roleType(RoleType.CASE)
            .grantType(GrantType.SPECIFIC)
            .attributes(tcAttributes)
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .roleType(RoleType.CASE)
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
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .roleType(RoleType.ORGANISATION)
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
            .roleType(RoleType.CASE)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stcAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stcAttributes)
            .authorisations(Arrays.asList("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .roleType(RoleType.CASE)
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
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .grantType(GrantType.STANDARD)
            .roleType(RoleType.ORGANISATION)
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
            .roleType(RoleType.CASE)
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
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .roleType(RoleType.CASE)
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
            .roleType(RoleType.CASE)
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
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE,
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(specificAttributes)
            .roleType(RoleType.CASE)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stdAttributes)
            .grantType(GrantType.STANDARD)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .roleType(RoleType.CASE)
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
            .roleType(RoleType.CASE)
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
            RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION,
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .attributes(tcAttributes)
            .roleType(RoleType.ORGANISATION)
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
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("pagination-role")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> defaultSort(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("sorting-role")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> mapRoleAssignments(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> attributes = Map.of(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .attributes(attributes)
            .build();
        roleAssignments.add(roleAssignment);
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
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

    @Builder
    private static class TaskQueryScenario {
        String scenarioName;
        int firstResult;
        int maxResults;
        List<RoleAssignment> roleAssignments;
        SearchTaskRequest searchTaskRequest;
        int expectedAmountOfTasksInResponse;
        int expectedTotalRecords;
        List<Object> expectedTaskDetails;
        List<ZonedDateTime> expectedDueDates;
    }
}
