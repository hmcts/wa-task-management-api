package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

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
            orQueries.add(searchQuery.build());
            return this;
        }

        public CamundaAndQueryBuilder andQuery(CamundaSearchExpression searchExpression) {
            List<CamundaSearchExpression> processVariables = new ArrayList<>(singleton(searchExpression));

            orQueries.add(new CamundaOrQuery(processVariables));
            return this;
        }

        public CamundaSearchQuery build() {

            map.put("orQueries", orQueries);
            return new CamundaSearchQuery(map);
        }

    }
}

