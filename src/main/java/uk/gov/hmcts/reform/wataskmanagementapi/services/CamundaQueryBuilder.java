package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

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

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter"})
@Service
public class CamundaQueryBuilder {

    public CamundaSearchQuery createQuery(SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameter> searchParametersMap = asEnumMap(searchTaskRequest);

        Map<String, List<String>> userQueries = createUserQueries(searchParametersMap.get(USER));

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

        return camundaQuery()
            .andQuery(userQueries)
            .andQuery(jurisdictionQueries)
            .andQuery(locationQueries)
            .andQuery(stateQueries)
            .build();

    }

    private CamundaOrQuery.CamundaOrQueryBuilder createProcessVariableQueriesFor(CamundaVariableDefinition key,
                                                                                 SearchParameter searchParameter) {
        Set<CamundaSearchExpression> jurisdictionExpressions = buildSearchExpressions(key.value(), searchParameter);
        return asOrQuery(jurisdictionExpressions);

    }

    private Map<String, List<String>> createUserQueries(SearchParameter userSearchParameter) {

        //Safe-guard
        if (userSearchParameter == null) {
            return null;
        }

        return Map.of("assigneeIn", userSearchParameter.getValues());
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

    public CamundaSearchQuery createCompletionQuery(String caseId, List<String> taskTypes) {
        CamundaOrQuery.CamundaOrQueryBuilder caseIdQueries = createProcessVariableQueriesFor(
            CamundaVariableDefinition.CCD_ID,
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
                                SearchOperator.IN, asList(ASSIGNED.value(),UNASSIGNED.value(),REFERRED.value())));

        return camundaQuery()
            .andQuery(caseIdQueries)
            .andQuery(taskTypeQueries)
            .andQuery(stateQueries)
            .build();

    }
}
