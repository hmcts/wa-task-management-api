package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting.CamundaProcessVariableSortingExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting.CamundaSortingExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.sorting.CamundaSortingParameters;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery.CamundaOrQueryBuilder.orQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery.CamundaAndQueryBuilder.camundaQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.REFERRED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.TooManyMethods"})
@Service
public class CamundaQueryBuilder {

    /**
     * Builds a search query using the orQueries and sorting if provided from the search task request.
     * This method is used when performing searches.
     *
     * @param searchTaskRequest the search taskRequest provided in the request.
     * @return a mapped search query as specified by camunda as CamundaSearchQuery.
     */
    public CamundaSearchQuery createQuery(SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameter> searchParametersMap = asEnumMap(searchTaskRequest);

        Map<String, List<String>> userQueries = createUserQueries(searchParametersMap.get(USER));
        List<CamundaSortingExpression> sortingQueries = createSortingQueries(searchTaskRequest.getSortingParameters());

        CamundaOrQuery.CamundaOrQueryBuilder jurisdictionQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.JURISDICTION,
            searchParametersMap.get(SearchParameterKey.JURISDICTION)
        );

        CamundaOrQuery.CamundaOrQueryBuilder locationQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.LOCATION,
            searchParametersMap.get(SearchParameterKey.LOCATION)
        );

        CamundaOrQuery.CamundaOrQueryBuilder stateQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.TASK_STATE,
            searchParametersMap.get(SearchParameterKey.STATE)
        );

        CamundaOrQuery.CamundaOrQueryBuilder caseIdQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.CASE_ID,
            searchParametersMap.get(SearchParameterKey.CASE_ID)
        );

        CamundaSearchQuery.CamundaAndQueryBuilder queries = camundaQuery()
            .andQuery(userQueries)
            .andQuery(jurisdictionQueries)
            .andQuery(locationQueries)
            .andQuery(stateQueries)
            .andQuery(caseIdQueries)
            .andSortingQuery(sortingQueries);

        //Safe-guard to avoid sending empty orQueries to camunda
        if (queries.getOrQueries().isEmpty()) {
            return null;
        }

        return queries.build();
    }


    /**
     * Builds a search query using the orQueries.
     * This method is used when searching for tasks that are auto completable.
     *
     * @param caseId    the case id.
     * @param taskTypes the task types.
     * @return a mapped search query as specified by camunda as CamundaSearchQuery
     */
    public CamundaSearchQuery createCompletionQuery(String caseId, List<String> taskTypes) {
        CamundaOrQuery.CamundaOrQueryBuilder caseIdQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.CASE_ID,
            new SearchParameter(SearchParameterKey.CASE_ID,
                SearchOperator.IN, singletonList(caseId))
        );

        CamundaOrQuery.CamundaOrQueryBuilder taskTypeQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.TYPE,
            new SearchParameter(SearchParameterKey.TASK_TYPE,
                SearchOperator.IN, taskTypes));

        CamundaOrQuery.CamundaOrQueryBuilder stateQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.TASK_STATE,
            new SearchParameter(SearchParameterKey.STATE,
                SearchOperator.IN, asList(ASSIGNED.value(), UNASSIGNED.value(), REFERRED.value())));

        return camundaQuery()
            .andQuery(taskTypeQueries)
            .andQuery(stateQueries)
            .andQuery(caseIdQueries)
            .build();

    }

    /**
     * Creates the sorting query if sorting parameters where provided.
     *
     * @param sortingParameters the sorting parameters specified in the request.
     * @return a list of CamundaSortingParameters as per camunda specification.
     */
    @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
    private List<CamundaSortingExpression> createSortingQueries(List<SortingParameter> sortingParameters) {

        //Safe-guard
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            return null;
        }

        List<CamundaSortingExpression> sortingQueries = sortingParameters.stream().map(param -> {
            if (SortField.DUE_DATE == param.getSortBy()) {
                return createTaskVariableSortExpression(param.getSortBy(), param.getSortOrder());
            } else {
                //It's a process variable
                return createProcessVariableSortExpression(param.getSortBy(), param.getSortOrder());
            }
        }).collect(Collectors.toList());

        return sortingQueries;
    }

    /**
     * Generic method to creates a process variable query for any process variable.
     *
     * @param key             the process variable key.
     * @param searchParameter the searchParameter as provided in the request.
     * @return CamundaQueryBuilder object with the process variable query.
     */
    private CamundaOrQuery.CamundaOrQueryBuilder createProcessVariableQueriesFor(CamundaVariableDefinition key,
                                                                                 SearchParameter searchParameter) {
        Set<CamundaSearchExpression> jurisdictionExpressions = buildSearchExpressions(key.value(), searchParameter);
        return asOrQuery(jurisdictionExpressions);

    }

    /**
     * Creates the query required to match on assignee if a user was specified in the request.
     *
     * @param userSearchParameter the searchParameter with key USER as provided in the request.
     * @return a map with key "assigneeIn" and a list of userIds to be used as look up.
     */
    private Map<String, List<String>> createUserQueries(SearchParameter userSearchParameter) {

        //Safe-guard
        if (userSearchParameter == null) {
            return null;
        }

        return Map.of("assigneeIn", userSearchParameter.getValues());
    }

    private CamundaSortingExpression createTaskVariableSortExpression(SortField sortBy, SortOrder sortOrder) {
        return new CamundaSortingExpression(
            sortBy.toString(),
            sortOrder.toString()
        );
    }

    private CamundaSortingExpression createProcessVariableSortExpression(SortField sortBy, SortOrder sortOrder) {
        return new CamundaProcessVariableSortingExpression(
            "processVariable",
            sortOrder.toString(),
            new CamundaSortingParameters(sortBy.toString(), "String")
        );
    }

    private EnumMap<SearchParameterKey, SearchParameter> asEnumMap(SearchTaskRequest searchTaskRequest) {
        EnumMap<SearchParameterKey, SearchParameter> map = new EnumMap<>(SearchParameterKey.class);
        searchTaskRequest.getSearchParameters()
            .forEach(request -> map.put(request.getKey(), request));

        return map;
    }

    private CamundaOrQuery.CamundaOrQueryBuilder asOrQuery(Set<CamundaSearchExpression> jurisdictionExpressions) {
        CamundaOrQuery.CamundaOrQueryBuilder orQuery = orQuery();
        for (CamundaSearchExpression jurisdictionExpression : jurisdictionExpressions) {
            orQuery.query(jurisdictionExpression);
        }
        return orQuery;
    }

    private Set<CamundaSearchExpression> buildSearchExpressions(String key, SearchParameter searchParameter) {

        return ofNullable(searchParameter)
            .map(SearchParameter::getValues)
            .orElse(emptyList())
            .stream()
            .map(v -> asCamundaExpression(key, searchParameter.getOperator(), v))
            .collect(Collectors.toSet());
    }

    private CamundaSearchExpression asCamundaExpression(String key, SearchOperator operator, String value) {
        return value == null ? null : new CamundaSearchExpression(key, toCamundaOperator(operator), value);
    }

    private String toCamundaOperator(SearchOperator operator) {
        switch (operator) {
            case IN:
                return CamundaOperator.EQUAL.toString();
            case BETWEEN:
            case BEFORE:
            case AFTER:
                throw new UnsupportedOperationException(
                    "Unsupported search operator [" + operator.toString() + "] used in search parameter");
            default:
                throw new IllegalStateException("Unexpected search operator value: " + operator.toString());
        }
    }

}
