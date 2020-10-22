package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;

class CamundaQueryBuilderTest {

    private CamundaQueryBuilder camundaQueryBuilder;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        camundaQueryBuilder = new CamundaQueryBuilder();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_OR_and_AND_queries()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("someJurisdiction", "anotherJurisdiction")),
            new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation")),
            new SearchParameter(STATE, SearchOperator.IN, asList("someState", "anotherState"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "\t\"queries\": {\n"
                          + "\t\t\"orQueries\": [\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"assigneeIn\": [\n"
                          + "\t\t\t\t\t\"someUser\",\n"
                          + "\t\t\t\t\t\"anotherUser\"\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"jurisdiction\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherJurisdiction\"\n"
                          + "\t\t\t\t\t},\n"
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
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someLocation\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherLocation\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someState\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherState\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t}\n"
                          + "\t\t]\n"
                          + "\t}\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_AND_queries()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("someJurisdiction", "anotherJurisdiction")),
            new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation")),
            new SearchParameter(STATE, SearchOperator.IN, asList("someState", "anotherState"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "\t\"queries\": {\n"
                          + "\t\t\"orQueries\": [\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"assigneeIn\": [\n"
                          + "\t\t\t\t\t\"someUser\",\n"
                          + "\t\t\t\t\t\"anotherUser\"\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"jurisdiction\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherJurisdiction\"\n"
                          + "\t\t\t\t\t},\n"
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
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someLocation\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherLocation\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someState\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherState\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t}\n"
                          + "\t\t]\n"
                          + "\t}\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "\t\"queries\": {\n"
                          + "\t\t\"orQueries\": [\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"assigneeIn\": [\n"
                          + "\t\t\t\t\t\"someUser\",\n"
                          + "\t\t\t\t\t\"anotherUser\"\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"someLocation\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"location\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"anotherLocation\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t}\n"
                          + "\t\t]\n"
                          + "\t}\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_throw_an_unsupported_exception_if_search_operator_is_not_supported() {


        SearchTaskRequest searchTaskRequestWithBetween = new SearchTaskRequest(asList(
            new SearchParameter(USER, SearchOperator.BETWEEN, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.BETWEEN, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithBetween))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [BETWEEN] used in search parameter")
            .hasNoCause();

        SearchTaskRequest searchTaskRequestWithAfter = new SearchTaskRequest(asList(
            new SearchParameter(USER, SearchOperator.AFTER, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.AFTER, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithAfter))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [AFTER] used in search parameter")
            .hasNoCause();

        SearchTaskRequest searchTaskRequestWithBefore = new SearchTaskRequest(asList(
            new SearchParameter(USER, SearchOperator.BEFORE, asList("someUser", "anotherUser")),
            new SearchParameter(LOCATION, SearchOperator.BEFORE, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithBefore))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [BEFORE] used in search parameter")
            .hasNoCause();
    }
}
