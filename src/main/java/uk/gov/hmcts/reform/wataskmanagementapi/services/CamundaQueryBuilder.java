package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery.CamundaOrQueryBuilder.orQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery.CamundaAndQueryBuilder.camundaQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter"})
@Service
public class CamundaQueryBuilder {

    public CamundaSearchQuery createQuery(SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameter> searchParametersMap = new EnumMap<>(SearchParameterKey.class);
        searchTaskRequest.getSearchParameters()
            .forEach(request -> searchParametersMap.put(request.getKey(), request));

        Set<CamundaSearchExpression> jurisdictionExpressions =
            asMultipleExpressions("jurisdiction", searchParametersMap.get(JURISDICTION));
        CamundaOrQuery.CamundaOrQueryBuilder jurisdictionQueries = asOrQuery(jurisdictionExpressions);

        Map<String, List<String>> userQueries = new ConcurrentHashMap<>();
        userQueries.put("assigneeIn", searchParametersMap.get(USER).getValues());

        Set<CamundaSearchExpression> locationExpressions =
            asMultipleExpressions("location", searchParametersMap.get(LOCATION));
        CamundaOrQuery.CamundaOrQueryBuilder locationQueries = asOrQuery(locationExpressions);

        Set<CamundaSearchExpression> stateQueriesExpressions =
            asMultipleExpressions("taskState", searchParametersMap.get(STATE));
        CamundaOrQuery.CamundaOrQueryBuilder stateQueries = asOrQuery(stateQueriesExpressions);

        return camundaQuery()
            .andQuery(userQueries)
            .andQuery(jurisdictionQueries)
            .andQuery(locationQueries)
            .andQuery(stateQueries)
            .build();

    }

    private CamundaOrQuery.CamundaOrQueryBuilder asOrQuery(Set<CamundaSearchExpression> jurisdictionExpressions) {
        CamundaOrQuery.CamundaOrQueryBuilder orQuery = orQuery();
        for (CamundaSearchExpression jurisdictionExpression : jurisdictionExpressions) {
            orQuery.query(jurisdictionExpression);
        }
        return orQuery;
    }

    private Set<CamundaSearchExpression> asMultipleExpressions(String key, SearchParameter searchParameter) {

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
