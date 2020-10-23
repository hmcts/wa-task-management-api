package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.ArrayList;
import java.util.List;

public class CamundaOrQuery {

    private final List<CamundaSearchExpression> processVariables;

    public CamundaOrQuery(List<CamundaSearchExpression> processVariables) {
        this.processVariables = processVariables;
    }

    public List<CamundaSearchExpression> getProcessVariables() {
        return processVariables;
    }

    public static class CamundaOrQueryBuilder {

        private final List<CamundaSearchExpression> processVariables = new ArrayList<>();

        public static CamundaOrQueryBuilder orQuery() {
            return new CamundaOrQueryBuilder();
        }

        public CamundaOrQueryBuilder query(CamundaSearchExpression searchQuery) {
            processVariables.add(searchQuery);
            return this;
        }

        public CamundaOrQuery build() {
            return new CamundaOrQuery(processVariables);
        }

    }
}

