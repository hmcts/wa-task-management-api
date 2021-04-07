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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void createQuery_should_return_null_when_orQueries_is_empty() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(emptyList());

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        assertNull(camundaSearchQuery);
    }

    @Test
    void createQuery_should_return_null_when_orQueries_is_empty_but_has_order_by() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            emptyList(),
            singletonList(
                new SortingParameter(SortField.DUE_DATE, SortOrder.DESCENDANT)
            ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        assertNull(camundaSearchQuery);
    }

    @Test
    void createQuery_should_build_query_for_task_state_unassigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, asList("unassigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"unassigned\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_for_task_state_assigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, asList("assigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"assigned\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_for_task_state_assigned_and_unassigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, asList("assigned", "unassigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"assigned\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"unassigned\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
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
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter_and_due_date_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            singletonList(
                new SortingParameter(SortField.DUE_DATE, SortOrder.DESCENDANT)
            )
        );

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"assigneeIn\": [\n"
                          + "          \"someUser\",\n"
                          + "          \"anotherUser\"\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someLocation\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherLocation\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"sorting\": [\n"
                          + "      {\n"
                          + "        \"sortBy\": \"dueDate\",\n"
                          + "        \"sortOrder\": \"desc\"\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_search_parameter_and_case_category_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            singletonList(
                new SortingParameter(SortField.CASE_CATEGORY, SortOrder.ASCENDANT)
            )
        );

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"assigneeIn\": [\n"
                          + "          \"someUser\",\n"
                          + "          \"anotherUser\"\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someLocation\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherLocation\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"sorting\": [\n"
                          + "      {\n"
                          + "        \"sortBy\": \"processVariable\",\n"
                          + "        \"sortOrder\": \"asc\",\n"
                          + "        \"parameters\": {\n"
                          + "          \"variable\": \"caseCategory\",\n"
                          + "          \"type\": \"String\"\n"
                          + "        }\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter_and_multiple_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameter(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameter(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            asList(
                new SortingParameter(SortField.DUE_DATE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID, SortOrder.DESCENDANT)
            )
        );

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"assigneeIn\": [\n"
                          + "          \"someUser\",\n"
                          + "          \"anotherUser\"\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"processVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someLocation\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherLocation\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"sorting\": [\n"
                          + "      {\n"
                          + "        \"sortBy\": \"dueDate\",\n"
                          + "        \"sortOrder\": \"desc\"\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"sortBy\": \"processVariable\",\n"
                          + "        \"sortOrder\": \"desc\",\n"
                          + "        \"parameters\": {\n"
                          + "          \"variable\": \"caseId\",\n"
                          + "          \"type\": \"String\"\n"
                          + "        }\n"
                          + "      }\n"
                          + "    ]\n"
                          + "  }\n"
                          + "}\n";

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


    @Test
    void createCompleteQuery_should_build_query_from_search_task_request_with_only_AND_queries()
        throws JsonProcessingException, JSONException {

        List<String> taskTypes = new ArrayList<>();
        taskTypes.add("Test Task Type");


        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createCompletableTasksQuery("caseId", taskTypes);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "\t\"queries\": {\n"
                          + "\t\t\"orQueries\": [\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"caseId\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"caseId\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskId\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"Test Task Type\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t},\n"
                          + "\t\t\t{\n"
                          + "\t\t\t\t\"processVariables\": [\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"assigned\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"referred\"\n"
                          + "\t\t\t\t\t},\n"
                          + "\t\t\t\t\t{\n"
                          + "\t\t\t\t\t\t\"name\": \"taskState\",\n"
                          + "\t\t\t\t\t\t\"operator\": \"eq\",\n"
                          + "\t\t\t\t\t\t\"value\": \"unassigned\"\n"
                          + "\t\t\t\t\t}\n"
                          + "\t\t\t\t]\n"
                          + "\t\t\t}\n"
                          + "\t\t]\n"
                          + "\t}\n"
                          + "}";
        JSONAssert.assertEquals(expected, resultJson, false);
    }

}
