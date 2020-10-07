package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery.CamundaAndQueryBuilder.camundaQuery;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOperators.EQUAL;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaOrQuery.CamundaOrQueryBuilder.orQuery;

class CamundaSearchQueryTest {

    @Test
    void test() {
        String eq = EQUAL.toString();
        CamundaSearchQuery a = camundaQuery()
            .andQuery(orQuery()
                          .query(new CamundaSearchExpression("ccdId", eq , "0002"))
                          .query(new CamundaSearchExpression("ccdId", eq, "0003")))
            .andQuery(orQuery()
                          .query(new CamundaSearchExpression("group", eq, "TCW")))
            .build();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(a.getQueries());
        System.out.println(jsonOutput);

    }
}
