package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
public class CamundaSearchQuery {

    private final Map<String, Object> queries;

    public CamundaSearchQuery(Map<String, Object> queries) {
        this.queries = queries;
    }

    public Map<String, Object> getQueries() {
        return queries;
    }

    public static class CamundaAndQueryBuilder {

        private final Map<String, Object> map = new ConcurrentHashMap<>();
        private final List<Object> orQueries = new ArrayList<>();

        public static CamundaAndQueryBuilder camundaQuery() {
            return new CamundaAndQueryBuilder();
        }

        public CamundaAndQueryBuilder withKeyValue(String key, String value) {
            map.put(key, singletonList(value));
            return this;
        }

        public CamundaAndQueryBuilder andQuery(CamundaOrQuery.CamundaOrQueryBuilder searchQuery) {
            CamundaOrQuery query = searchQuery.build();
            if (!query.getProcessVariables().isEmpty()) {
                orQueries.add(query);
            }
            return this;
        }

        public CamundaAndQueryBuilder andQuery(CamundaSearchExpression searchExpression) {
            if (searchExpression != null) {
                List<CamundaSearchExpression> processVariables = new ArrayList<>(singleton(searchExpression));
                orQueries.add(new CamundaOrQuery(processVariables));
            }
            return this;
        }

        public CamundaAndQueryBuilder andQuery(Map<String, List<String>> searchExpression) {
            if (searchExpression != null && !searchExpression.isEmpty()) {
                orQueries.add(searchExpression);
            }
            return this;
        }

        public CamundaSearchQuery build() {
            map.put("orQueries", orQueries);
            return new CamundaSearchQuery(map);
        }

    }
}

