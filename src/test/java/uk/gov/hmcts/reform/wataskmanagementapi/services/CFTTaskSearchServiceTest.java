package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_SEARCH_INDEX_SEARCH_ENABLED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CFTTaskSearchServiceTest {

    private static final String SERVICE_USER_ID = "wa-task-management-api";
    private static final String SERVICE_EMAIL = "wa-task-management-api@hmcts.net";
    private static final String EXCLUDED_CASE_ID = "case-excluded";

    @Mock
    private TaskResourceRepository tasksRepository;
    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private CFTTaskSearchService cftTaskSearchService;

    @BeforeEach
    void setUp() {
        cftTaskSearchService = new CFTTaskSearchService(tasksRepository, launchDarklyFeatureFlagProvider);
    }

    @Test
    void should_use_legacy_search_index_when_feature_flag_enabled() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(ASSIGNED))
            .jurisdictions(List.of("IA"))
            .regions(List.of("1"))
            .locations(List.of("765324"))
            .build();
        List<RoleAssignment> roleAssignments = List.of(
            roleAssignment(
                "hmcts-judiciary",
                Classification.PUBLIC,
                GrantType.STANDARD,
                Map.of(
                    RoleAttributeDefinition.JURISDICTION.value(), "IA",
                    RoleAttributeDefinition.REGION.value(), "1",
                    RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
                ),
                List.of()
            ),
            roleAssignment(
                "excluded-role",
                Classification.PUBLIC,
                GrantType.EXCLUDED,
                Map.of(RoleAttributeDefinition.CASE_ID.value(), EXCLUDED_CASE_ID),
                List.of()
            )
        );

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            WA_SEARCH_INDEX_SEARCH_ENABLED,
            SERVICE_USER_ID,
            SERVICE_EMAIL
        )).thenReturn(true);
        when(tasksRepository.searchTasksIdsOld(
            eq(1), eq(25), anySet(), anySet(), eq(List.of(EXCLUDED_CASE_ID)), eq(searchRequest)
        )).thenReturn(List.of("task-1"));
        when(tasksRepository.searchTasksCountOld(
            anySet(), anySet(), eq(List.of(EXCLUDED_CASE_ID)), eq(searchRequest)
        )).thenReturn(1L);

        CFTTaskSearchService.SearchResult result = cftTaskSearchService.searchForTaskIds(
            1, 25, searchRequest, roleAssignments
        );

        assertThat(result.taskIds()).containsExactly("task-1");
        assertThat(result.totalRecords()).isEqualTo(1);

        ArgumentCaptor<Set<String>> filterSignatureCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<String>> roleSignatureCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tasksRepository).searchTasksIdsOld(
            eq(1),
            eq(25),
            filterSignatureCaptor.capture(),
            roleSignatureCaptor.capture(),
            eq(List.of(EXCLUDED_CASE_ID)),
            eq(searchRequest)
        );
        assertThat(filterSignatureCaptor.getValue()).containsExactly("A:IA:*:*:1:765324");
        assertThat(roleSignatureCaptor.getValue()).containsExactly("IA:1:765324:hmcts-judiciary:*:r:U:*");
        verify(tasksRepository, never()).searchTasksIds(
            eq(1), eq(25), anyCollection(), anyList(), eq(searchRequest)
        );
    }

    @Test
    void should_use_new_role_criteria_search_when_feature_flag_disabled() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.ALL_WORK)
            .build();
        List<RoleAssignment> roleAssignments = List.of(
            roleAssignment(
                "task-supervisor",
                Classification.RESTRICTED,
                GrantType.STANDARD,
                Map.of(),
                List.of("skill-1")
            ),
            roleAssignment(
                "excluded-role",
                Classification.PUBLIC,
                GrantType.EXCLUDED,
                Map.of(RoleAttributeDefinition.CASE_ID.value(), EXCLUDED_CASE_ID),
                List.of()
            )
        );

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            WA_SEARCH_INDEX_SEARCH_ENABLED,
            SERVICE_USER_ID,
            SERVICE_EMAIL
        )).thenReturn(false);
        when(tasksRepository.searchTasksIds(
            eq(2), eq(50), anyCollection(), eq(List.of(EXCLUDED_CASE_ID)), eq(searchRequest)
        )).thenReturn(List.of("task-2", "task-3"));
        when(tasksRepository.searchTasksCount(
            anyCollection(), eq(List.of(EXCLUDED_CASE_ID)), eq(searchRequest)
        )).thenReturn(2L);

        CFTTaskSearchService.SearchResult result = cftTaskSearchService.searchForTaskIds(
            2, 50, searchRequest, roleAssignments
        );

        assertThat(result.taskIds()).containsExactly("task-2", "task-3");
        assertThat(result.totalRecords()).isEqualTo(2);

        ArgumentCaptor<Collection<TaskSearchRoleCriteria>> roleCriteriaCaptor = ArgumentCaptor.forClass(
            Collection.class
        );
        verify(tasksRepository).searchTasksIds(
            eq(2),
            eq(50),
            roleCriteriaCaptor.capture(),
            eq(List.of(EXCLUDED_CASE_ID)),
            eq(searchRequest)
        );
        assertThat(roleCriteriaCaptor.getValue())
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
        verify(tasksRepository, never()).searchTasksIdsOld(
            eq(2), eq(50), anySet(), anySet(), anyList(), eq(searchRequest)
        );
    }

    @Test
    void should_build_available_task_role_criteria_with_authorizations_and_wildcard() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .build();
        RoleAssignment roleAssignment = roleAssignment(
            "tribunal-caseworker",
            Classification.PRIVATE,
            GrantType.STANDARD,
            Map.of(RoleAttributeDefinition.JURISDICTION.value(), "IA"),
            List.of("skill-1", "skill-2", "skill-1")
        );

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            WA_SEARCH_INDEX_SEARCH_ENABLED,
            SERVICE_USER_ID,
            SERVICE_EMAIL
        )).thenReturn(false);
        when(tasksRepository.searchTasksIds(eq(0), eq(25), anyCollection(), eq(List.of()), eq(searchRequest)))
            .thenReturn(List.of());

        cftTaskSearchService.searchForTaskIds(0, 25, searchRequest, List.of(roleAssignment));

        ArgumentCaptor<Collection<TaskSearchRoleCriteria>> roleCriteriaCaptor = ArgumentCaptor.forClass(
            Collection.class
        );
        verify(tasksRepository).searchTasksIds(
            eq(0),
            eq(25),
            roleCriteriaCaptor.capture(),
            eq(List.of()),
            eq(searchRequest)
        );
        assertThat(roleCriteriaCaptor.getValue())
            .containsExactly(
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", null),
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", "skill-1"),
                new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseworker", null, "a", "P", "skill-2")
            );
        verify(tasksRepository, never()).searchTasksCount(anyCollection(), anyList(), eq(searchRequest));
    }

    @Test
    void should_not_count_legacy_search_when_no_task_ids_are_returned() {
        SearchRequest searchRequest = SearchRequest.builder().build();
        RoleAssignment roleAssignment = roleAssignment(
            "hmcts-judiciary",
            Classification.PUBLIC,
            GrantType.STANDARD,
            Map.of(),
            List.of()
        );

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            WA_SEARCH_INDEX_SEARCH_ENABLED,
            SERVICE_USER_ID,
            SERVICE_EMAIL
        )).thenReturn(true);
        when(tasksRepository.searchTasksIdsOld(eq(0), eq(25), anySet(), anySet(), eq(List.of()), eq(searchRequest)))
            .thenReturn(List.of());

        CFTTaskSearchService.SearchResult result = cftTaskSearchService.searchForTaskIds(
            0, 25, searchRequest, List.of(roleAssignment)
        );

        assertThat(result.taskIds()).isEmpty();
        assertThat(result.totalRecords()).isZero();
        verify(tasksRepository, never()).searchTasksCountOld(anySet(), anySet(), anyList(), eq(searchRequest));
    }

    private RoleAssignment roleAssignment(String roleName,
                                          Classification classification,
                                          GrantType grantType,
                                          Map<String, String> attributes,
                                          List<String> authorisations) {
        return RoleAssignment.builder()
            .roleName(roleName)
            .classification(classification)
            .grantType(grantType)
            .attributes(attributes)
            .authorisations(authorisations)
            .build();
    }
}
