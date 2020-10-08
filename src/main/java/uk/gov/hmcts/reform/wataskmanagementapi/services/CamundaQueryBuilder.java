package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.SearchParameters;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOperators;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchExpression;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery.CamundaOrQueryBuilder.orQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery.CamundaAndQueryBuilder.camundaQuery;

@Service
public class CamundaQueryBuilder {

    public CamundaSearchQuery createQuery(SearchTaskRequest searchTaskRequest) {

        String operator = CamundaOperators.EQUAL.toString();

        SearchParameters searchParameters = searchTaskRequest.getSearchParameters().get(0);

        Set<CamundaSearchExpression> jurisdictionExpressions =
            asMultipleExpressions("jurisdiction", operator, searchParameters.getJurisdiction());
        CamundaOrQuery.CamundaOrQueryBuilder jurisdictionQueries = asOrQuery(jurisdictionExpressions);

        Set<CamundaSearchExpression> userExpressions =
            asMultipleExpressions("userId", operator, searchParameters.getUser());
        CamundaOrQuery.CamundaOrQueryBuilder userQueries = asOrQuery(userExpressions);

        Set<CamundaSearchExpression> locationExpressions =
            asMultipleExpressions("location", operator, searchParameters.getLocation());
        CamundaOrQuery.CamundaOrQueryBuilder locationQueries = asOrQuery(locationExpressions);

        Set<CamundaSearchExpression> stateQueriesExpressions =
            asMultipleExpressions("state", operator, searchParameters.getState());
        CamundaOrQuery.CamundaOrQueryBuilder stateQueries = asOrQuery(stateQueriesExpressions);

        CamundaSearchExpression ccdIdQuery =
            asCamundaExpression("ccdId", operator, searchParameters.getCcdId());
        CamundaSearchExpression eventIdQuery =
            asCamundaExpression("eventId", operator, searchParameters.getEventId());
        CamundaSearchExpression preEventQuery =
            asCamundaExpression("preEventState", operator, searchParameters.getPreEventState());
        CamundaSearchExpression postEventQuery =
            asCamundaExpression("postEventState", operator, searchParameters.getPostEventState());


        return camundaQuery()
            .andQuery(jurisdictionQueries)
            .andQuery(userQueries)
            .andQuery(locationQueries)
            .andQuery(stateQueries)
            .andQuery(ccdIdQuery)
            .andQuery(eventIdQuery)
            .andQuery(preEventQuery)
            .andQuery(postEventQuery)
            .build();

    }

    private CamundaOrQuery.CamundaOrQueryBuilder asOrQuery(Set<CamundaSearchExpression> jurisdictionExpressions) {
        CamundaOrQuery.CamundaOrQueryBuilder orQuery = orQuery();
        for (CamundaSearchExpression jurisdictionExpression : jurisdictionExpressions) {
            orQuery.query(jurisdictionExpression);
        }
        return orQuery;
    }

    private Set<CamundaSearchExpression> asMultipleExpressions(String key, String operator, List<String> values) {
        return values.stream()
            .map(v -> asCamundaExpression(key, operator, v))
            .collect(Collectors.toSet());
    }

    private CamundaSearchExpression asCamundaExpression(String key, String operator, String value) {
        return new CamundaSearchExpression(key, operator, value);
    }
}
