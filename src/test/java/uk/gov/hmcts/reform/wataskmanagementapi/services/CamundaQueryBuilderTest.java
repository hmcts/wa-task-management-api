package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.SearchParameters;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;

import static java.util.Collections.singletonList;

class CamundaQueryBuilderTest {

    private CamundaQueryBuilder camundaQueryBuilder;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        camundaQueryBuilder = new CamundaQueryBuilder();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createQuery_should_build_query_from_search_request() throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = buildSearchTaskRequest();
        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "\t\"queries\": {\n"
                          + "\t\t\"orQueries\": [\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"jurisdiction\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someJurisdiction\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"userId\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someUser\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someLocation\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someState\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"ccdId\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someCcdId\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"eventId\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someEventId\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"preEventState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"somePreEventState\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"postEventState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"somePostEventState\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t}\n"
                          + "\t\t]\n"
                          + "\t}\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    private SearchTaskRequest buildSearchTaskRequest() {

        SearchParameters searchParameters = new SearchParameters(
            singletonList("someJurisdiction"),
            singletonList("someUser"),
            singletonList("someLocation"),
            singletonList("someState"),
            "someCcdId",
            "someEventId",
            "somePreEventState",
            "somePostEventState"
        );

        return new SearchTaskRequest(singletonList(searchParameters));
    }
}
