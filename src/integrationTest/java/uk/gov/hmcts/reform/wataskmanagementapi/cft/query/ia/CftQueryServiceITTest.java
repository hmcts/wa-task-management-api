package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequestMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestIndexUtils;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.launchdarkly.shaded.com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.LOCATION;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/ia/data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class CftQueryServiceITTest extends RoleAssignmentHelper {

    @MockitoBean
    private CamundaService camundaService;

    private CftQueryService cftQueryService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    private CFTTaskDatabaseService cftTaskDatabaseService;

    IntegrationTestIndexUtils integrationTestIndexUtils = new IntegrationTestIndexUtils();

    private static final UserInfo userInfo = UserInfo.builder().email("user@test.com").uid("user").build();

    @Autowired
    TaskResourceRepository taskResourceRepository;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
            allowedJurisdictionConfiguration
        );

        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);
    }

    @ParameterizedTest()
    @MethodSource({
        "withAllGrantTypesHappyPath",
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
        "secondaryCaseCategorySortedResultsDescScenario",
        "grantTypeWithAllWorkOnRequestContextScenarioHappyPath",
        "grantTypeWithAvailableTasksOnRequestContextScenarioHappyPath"
    })
    void shouldRetrieveTasks(TaskQueryScenario scenario) {
        log.info("Running scenario: {}", scenario.scenarioName);

        //given
        AccessControlResponse accessControlResponse = new AccessControlResponse(scenario.userInfo,
            scenario.roleAssignments);
        SearchRequest searchRequest = SearchTaskRequestMapper.map(scenario.searchTaskRequest);
        integrationTestIndexUtils.indexRecord(taskResourceRepository);
        //when
        final GetTasksResponse<Task> allTasks = cftTaskDatabaseService.searchForTasks(
            scenario.firstResult,
            scenario.maxResults,
            searchRequest,
            accessControlResponse
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

    private static Stream<TaskQueryScenario> grantTypeWithAvailableTasksOnRequestContextScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")))
        );

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks should return only unassigned and OWN and ClAIM permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .granularPermission(true)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario privateClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks should return only unassigned and OWN and ClAIM permission and PRIVATE")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(2)
            .expectedTotalRecords(2)
            .userInfo(userInfo)
            .granularPermission(true)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111023", "1623278362431023",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("available_tasks should return only unassigned and OWN and ClAIM permission "
                          + "excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(userInfo)
            .granularPermission(true)
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

    private static Stream<TaskQueryScenario> grantTypeWithAllWorkOnRequestContextScenarioHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of(IA_JURISDICTION)),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")))
        );

        final TaskQueryScenario publicClassification = TaskQueryScenario.builder()
            .scenarioName("all_work should return only unassigned and MANAGE permission and PUBLIC")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(1)
            .expectedTotalRecords(1)
            .userInfo(userInfo)
            .granularPermission(true)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null
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
            .userInfo(userInfo)
            .granularPermission(true)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                    "TestCase2", "Taylor House", "title", null
                )
            ).build();

        final TaskQueryScenario restrictedClassification = TaskQueryScenario.builder()
            .scenarioName("all_work should return only unassigned and MANAGE permission "
                          + "excluded_grant_type_with_classification_as_restricted")
            .firstResult(0)
            .maxResults(10)
            .roleAssignments(roleAssignmentsWithGrantTypeStandard(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedAmountOfTasksInResponse(3)
            .expectedTotalRecords(3)
            .userInfo(userInfo)
            .granularPermission(true)
            .expectedTaskDetails(newArrayList(
                    "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005",
                    "TestCase3", "Taylor House", "title", null,
                    "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                    "TestCase2", "Taylor House", "title", null,
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
            .expectedTotalRecords(13)
            .userInfo(userInfo)
            .expectedTaskDetails(newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005", "TestCase3",
                "Taylor House", "title", null,
                "8d6cc5cf-c973-11eb-bdba-0242ac111025", "1623278362431025", "TestCase3",
                "Taylor House", "title", null,
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004", "TestCase2",
                "Taylor House", "title", null,
                "8d6cc5cf-c973-11eb-bdba-0242ac111024", "1623278362431024", "TestCase2",
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
                "Taylor House", "title", null,
                "8d6cc5cf-c973-11eb-bdba-0242ac111027", "1623278362431027", "TestCase4",
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .userInfo(userInfo)
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    private static List<RoleAssignment> roleAssignmentWithAllGrantTypes(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .attributes(Collections.emptyMap())
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
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
            .attributes(Collections.emptyMap())
            .beginTime(OffsetDateTime.now().minusYears(1))
            .endTime(OffsetDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
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
        UserInfo userInfo;
        boolean granularPermission;

        @Override
        public String toString() {
            return scenarioName;
        }

    }
}
