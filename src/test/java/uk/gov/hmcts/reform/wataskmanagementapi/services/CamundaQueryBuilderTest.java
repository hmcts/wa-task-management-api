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
        String expected = "{\"queries\":{\"orQueries\":[{\"processVariables\":[{\"name\":\"jurisdiction\",\"operator\":\"eq\",\"value\":\"someJurisdiction\"}]},{\"processVariables\":[{\"name\":\"userId\",\"operator\":\"eq\",\"value\":\"someUser\"}]},{\"processVariables\":[{\"name\":\"location\",\"operator\":\"eq\",\"value\":\"someLocation\"}]},{\"processVariables\":[{\"name\":\"state\",\"operator\":\"eq\",\"value\":\"someState\"}]},{\"processVariables\":[{\"name\":\"ccdId\",\"operator\":\"eq\",\"value\":\"someCcdId\"}]},{\"processVariables\":[{\"name\":\"eventId\",\"operator\":\"eq\",\"value\":\"someEventId\"}]},{\"processVariables\":[{\"name\":\"preEventState\",\"operator\":\"eq\",\"value\":\"somePreEventState\"}]},{\"processVariables\":[{\"name\":\"postEventState\",\"operator\":\"eq\",\"value\":\"somePostEventState\"}]}]}}";

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
