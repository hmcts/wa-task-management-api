package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID_CAMEL_CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.WORK_TYPE;

public class SearchTaskRequestMapperTest {

    @Test
    void shouldMapAllListValues() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA", "WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324", "765325")),
                new SearchParameterList(STATE, SearchOperator.IN,
                    Arrays.stream(CFTTaskState.values()).map(CFTTaskState::getValue).collect(Collectors.toList())),
                new SearchParameterList(USER, SearchOperator.IN, asList("User1", "User2")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003", "1623278362432003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, SearchTaskRequestMapper.ALLOWED_WORK_TYPES),
                new SearchParameterList(TASK_TYPE, SearchOperator.IN, List.of("processApplication", "reviewAppeal")),
                new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN,
                    Arrays.stream(RoleCategory.values()).map(RoleCategory::toString).collect(Collectors.toList()))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.DESCENDANT))
        );
        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        assertThat(searchRequest.getLocations(), hasItems("765324", "765325"));
        assertThat(searchRequest.getUsers(), hasItems("User1", "User2"));
        assertThat(searchRequest.getJurisdictions(), hasItems("IA", "WA"));
        assertThat(searchRequest.getCftTaskStates(), hasItems(CFTTaskState.values()));
        assertThat(searchRequest.getTaskTypes(), hasItems("processApplication", "reviewAppeal"));
        assertThat(searchRequest.getCaseIds(), hasItems("1623278362431003", "1623278362432003"));
        assertThat(searchRequest.getWorkTypes(), hasItems(SearchTaskRequestMapper.ALLOWED_WORK_TYPES
            .toArray(new String[0])));
        assertThat(searchRequest.getRoleCategories(), hasItems(RoleCategory.values()));
        assertThat(searchRequest.getSortingParameters(),
            hasItems(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.DESCENDANT)));
    }

    @Test
    void shouldMapAvailableTaskOnly() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("ia")))
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        assertTrue(searchRequest.isAvailableTasksOnly());
        assertThat(searchRequest.getCftTaskStates(), hasItem(CFTTaskState.UNASSIGNED));

    }

    @Test
    void shouldMapCamelCaseCaseId() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA", "WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324", "765325")),
                new SearchParameterList(STATE, SearchOperator.IN,
                    Arrays.stream(CFTTaskState.values()).map(CFTTaskState::getValue).collect(Collectors.toList())),
                new SearchParameterList(USER, SearchOperator.IN, asList("User1", "User2")),
                new SearchParameterList(CASE_ID_CAMEL_CASE, SearchOperator.IN, asList("1623278362431003",
                    "1623278362432003"))
            )
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        assertThat(searchRequest.getLocations(), hasItems("765324", "765325"));
        assertThat(searchRequest.getUsers(), hasItems("User1", "User2"));
        assertThat(searchRequest.getJurisdictions(), hasItems("IA", "WA"));
        assertThat(searchRequest.getCftTaskStates(), hasItems(CFTTaskState.UNASSIGNED));
        assertThat(searchRequest.getCaseIds(), hasItems("1623278362431003", "1623278362432003"));
        assertTrue(searchRequest.isAvailableTasksOnly());
    }

    @Test
    void shouldMapEmptyRequest() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of()
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        assertFalse(searchRequest.isAvailableTasksOnly());
        assertThat(searchRequest.getLocations(), hasSize(0));
        assertThat(searchRequest.getUsers(), hasSize(0));
        assertThat(searchRequest.getJurisdictions(), hasSize(0));
        assertThat(searchRequest.getCftTaskStates(), hasSize(0));
        assertThat(searchRequest.getTaskTypes(), hasSize(0));
        assertThat(searchRequest.getCaseIds(), hasSize(0));
        assertThat(searchRequest.getWorkTypes(), hasSize(0));
        assertThat(searchRequest.getRoleCategories(), hasSize(0));
        assertThat(searchRequest.getSortingParameters(), hasSize(0));
    }

    @Test
    void shouldValidateWorkType() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, asList("Invalid_work_type"))
            )
        );

        CustomConstraintViolationException thrown = assertThrows(CustomConstraintViolationException.class, () -> {
            SearchTaskRequestMapper.map(searchTaskRequest);
        });

        assertEquals("Constraint Violation", thrown.getMessage());
        assertEquals("Invalid_work_type", thrown.getViolations().get(0).getField());
        assertEquals("work_type must be one of " + Arrays.toString(SearchTaskRequestMapper
            .ALLOWED_WORK_TYPES.toArray()), thrown.getViolations().get(0).getMessage());
    }
}
