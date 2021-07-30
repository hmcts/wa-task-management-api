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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;

public class CftQueryServiceTest extends CftRepositoryBaseTest {

    private List<PermissionTypes> permissionsRequired = new ArrayList<>();

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        cftQueryService = new CftQueryService(taskResourceRepository);

    }

    @ParameterizedTest
    @MethodSource("grantTypeBasicScenarioProviderHappyPath")
    @Sql("/scripts/data.sql")
    void shouldRetrieveTasksWhenGrantTypeIsBasic(GrantTypeScenario scenario) {
        //given
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final List<TaskResource> allTasks = cftQueryService.getAllTasks(
            scenario.searchTaskRequest, accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks)
            .hasSize(scenario.expectedSize)
            .flatExtracting(TaskResource::getTaskId, TaskResource::getCaseId)
            .containsExactlyInAnyOrder(
                scenario.expectedTaskDetails.toArray()
            );
    }

    @ParameterizedTest
    @MethodSource("invalidExUiSearchQuery")
    @Sql("/scripts/data.sql")
    void shouldReturnEmptyTasksWithInvalidExUiSearchQuery(GrantTypeScenario scenario) {
        //given
        mapRoleAssignments(Classification.PUBLIC);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, scenario.roleAssignments);
        permissionsRequired.add(PermissionTypes.READ);

        //when
        final List<TaskResource> allTasks = cftQueryService.getAllTasks(
            scenario.searchTaskRequest, accessControlResponse, permissionsRequired
        );

        //then
        Assertions.assertThat(allTasks)
            .isEmpty();
    }

    @Builder
    private static class GrantTypeScenario {
        List<RoleAssignment> roleAssignments;
        SearchTaskRequest searchTaskRequest;
        int expectedSize;
        List<String> expectedTaskDetails;
    }

    private static Stream<GrantTypeScenario> grantTypeBasicScenarioProviderHappyPath() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        final GrantTypeScenario withJurisdiction = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003")
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324"))
        ));

        final GrantTypeScenario withLocation = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003")
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(USER, SearchOperator.IN, asList("SELF"))
        ));

        final GrantTypeScenario withUser = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003")
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(STATE, SearchOperator.IN, asList("ASSIGNED"))
        ));

        final GrantTypeScenario withState = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003")
            ).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, asList("ASSIGNED")),
            new SearchParameter(USER, SearchOperator.IN, asList("SELF"))
        ));

        final GrantTypeScenario publicClassification = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(2)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                    "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003")
            ).build();

        final GrantTypeScenario privateClassification = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PRIVATE))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(4)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004")
            ).build();

        final GrantTypeScenario restrictedClassification = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.RESTRICTED))
            .searchTaskRequest(searchTaskRequest)
            .expectedSize(6)
            .expectedTaskDetails(Lists.newArrayList(
                "8d6cc5cf-c973-11eb-bdba-0242ac111000", "1623278362431000",
                "8d6cc5cf-c973-11eb-bdba-0242ac111001", "1623278362431001",
                "8d6cc5cf-c973-11eb-bdba-0242ac111002", "1623278362431002",
                "8d6cc5cf-c973-11eb-bdba-0242ac111003", "1623278362431003",
                "8d6cc5cf-c973-11eb-bdba-0242ac111004", "1623278362431004",
                "8d6cc5cf-c973-11eb-bdba-0242ac111005", "1623278362431005"
                )
            ).build();

        return Stream.of(
            withJurisdiction,
            withLocation,
            withUser,
            withState,
            publicClassification,
            privateClassification,
            restrictedClassification
        );
    }

    private static Stream<GrantTypeScenario> invalidExUiSearchQuery() {
        final GrantTypeScenario invalidJurisdiction = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(getSearchTaskRequest("IA1", null, "ASSIGNED", null))
            .build();

        final GrantTypeScenario invalidLocation = GrantTypeScenario.builder()
            .roleAssignments(mapRoleAssignments(Classification.PUBLIC))
            .searchTaskRequest(getSearchTaskRequest("IA", "12345", "ASSIGNED", ""))
            .build();

        final GrantTypeScenario invalidUser = GrantTypeScenario.builder()
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
            new SearchParameter(USER, SearchOperator.IN, asList(assignee))
        ));
        return searchTaskRequest;
    }

    private static List<RoleAssignment> mapRoleAssignments(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(classification).build();
        roleAssignments.add(roleAssignment);
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification).build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }
}
