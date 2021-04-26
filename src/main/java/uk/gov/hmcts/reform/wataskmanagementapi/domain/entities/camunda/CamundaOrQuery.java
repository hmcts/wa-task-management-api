package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.ArrayList;
import java.util.List;

public class CamundaOrQuery {

    private final List<CamundaSearchExpression> taskVariables;

    public CamundaOrQuery(List<CamundaSearchExpression> taskVariables) {
        this.taskVariables = taskVariables;
    }

    public List<CamundaSearchExpression> getTaskVariables() {
        return taskVariables;
    }

    public static class CamundaOrQueryBuilder {

        private final List<CamundaSearchExpression> taskVariables = new ArrayList<>();

        public static CamundaOrQueryBuilder orQuery() {
            return new CamundaOrQueryBuilder();
        }

        public CamundaOrQueryBuilder query(CamundaSearchExpression searchQuery) {
            taskVariables.add(searchQuery);
            return this;
        }

        public CamundaOrQuery build() {
            return new CamundaOrQuery(taskVariables);
        }

    }
}

