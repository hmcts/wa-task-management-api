package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;

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

    @SuppressWarnings("JUnit5MalformedParameterized")
    @ParameterizedTest
    @EnumSource(names = {"DUE_DATE_CAMEL_CASE", "DUE_DATE_SNAKE_CASE"})
    void createQuery_should_return_null_when_orQueries_is_empty_but_has_order_by(SortField sortField) {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            emptyList(),
            singletonList(
                new SortingParameter(sortField, SortOrder.DESCENDANT)
            ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        assertNull(camundaSearchQuery);
    }

    @Test
    void createQuery_should_build_query_for_task_state_unassigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"unassigned\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_for_task_state_assigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, asList("assigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"assigned\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_for_task_state_assigned_and_unassigned()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, asList("assigned", "unassigned"))
        ));

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);

        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
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
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_OR_and_AND_queries()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("someJurisdiction", "anotherJurisdiction")),
            new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation")),
            new SearchParameterList(STATE, SearchOperator.IN, asList("someState", "anotherState"))
        ));

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
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"jurisdiction\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someJurisdiction\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"jurisdiction\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherJurisdiction\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherLocation\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"location\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someLocation\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherState\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someState\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_AND_queries()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("someJurisdiction", "anotherJurisdiction")),
            new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation")),
            new SearchParameterList(STATE, SearchOperator.IN, asList("someState", "anotherState"))
        ));

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
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"jurisdiction\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someJurisdiction\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"jurisdiction\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherJurisdiction\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
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
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherState\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"taskState\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"someState\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
        ));

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
                          + "        \"taskVariables\": [\n"
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
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter_and_due_date_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            singletonList(
                new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT)
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
                          + "        \"taskVariables\": [\n"
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
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_one_caseId_parameter_and_due_date_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(
                new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("aCaseId"))
            ),
            singletonList(
                new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT)
            )
        );

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"aCaseId\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"sorting\": [\n"
                          + "      {\n"
                          + "        \"sortBy\": \"dueDate\",\n"
                          + "        \"sortOrder\": \"desc\"\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }


    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_caseId_list_parameter_and_due_date_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("aCaseId", "anotherCaseId"))
            ),
            singletonList(
                new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT)
            )
        );

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createQuery(searchTaskRequest);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"aCaseId\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherCaseId\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"sorting\": [\n"
                          + "      {\n"
                          + "        \"sortBy\": \"dueDate\",\n"
                          + "        \"sortOrder\": \"desc\"\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_search_parameter_and_case_category_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            singletonList(
                new SortingParameter(SortField.CASE_CATEGORY_CAMEL_CASE, SortOrder.ASCENDANT)
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
                          + "        \"taskVariables\": [\n"
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
                          + "        \"sortBy\": \"taskVariable\",\n"
                          + "        \"sortOrder\": \"asc\",\n"
                          + "        \"parameters\": {\n"
                          + "          \"variable\": \"appealType\",\n"
                          + "          \"type\": \"String\"\n"
                          + "        }\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_only_specified_parameter_and_multiple_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            asList(
                new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID_CAMEL_CASE, SortOrder.DESCENDANT)
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
                          + "        \"taskVariables\": [\n"
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
                          + "        \"sortBy\": \"taskVariable\",\n"
                          + "        \"sortOrder\": \"desc\",\n"
                          + "        \"parameters\": {\n"
                          + "          \"variable\": \"caseId\",\n"
                          + "          \"type\": \"String\"\n"
                          + "        }\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_build_query_from_search_task_request_with_multiple_parameters_and_multiple_sorting()
        throws JsonProcessingException, JSONException {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("aCaseId", "anotherCaseId")),
                new SearchParameterList(USER, SearchOperator.IN, asList("someUser", "anotherUser")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("someLocation", "anotherLocation"))
            ),
            asList(
                new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT),
                new SortingParameter(SortField.CASE_ID_CAMEL_CASE, SortOrder.DESCENDANT)
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
                          + "        \"taskVariables\": [\n"
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
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"aCaseId\"\n"
                          + "          },\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"anotherCaseId\"\n"
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
                          + "        \"sortBy\": \"taskVariable\",\n"
                          + "        \"sortOrder\": \"desc\",\n"
                          + "        \"parameters\": {\n"
                          + "          \"variable\": \"caseId\",\n"
                          + "          \"type\": \"String\"\n"
                          + "        }\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

    @Test
    void createQuery_should_throw_an_unsupported_exception_if_search_operator_is_not_supported() {


        SearchTaskRequest searchTaskRequestWithBetween = new SearchTaskRequest(asList(
            new SearchParameterList(USER, SearchOperator.BETWEEN, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.BETWEEN, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithBetween))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [BETWEEN] used in search parameter")
            .hasNoCause();

        SearchTaskRequest searchTaskRequestWithAfter = new SearchTaskRequest(asList(
            new SearchParameterList(USER, SearchOperator.AFTER, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.AFTER, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithAfter))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [AFTER] used in search parameter")
            .hasNoCause();

        SearchTaskRequest searchTaskRequestWithBefore = new SearchTaskRequest(asList(
            new SearchParameterList(USER, SearchOperator.BEFORE, asList("someUser", "anotherUser")),
            new SearchParameterList(LOCATION, SearchOperator.BEFORE, asList("someLocation", "anotherLocation"))
        ));

        assertThatThrownBy(() -> camundaQueryBuilder.createQuery(searchTaskRequestWithBefore))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unsupported search operator [BEFORE] used in search parameter")
            .hasNoCause();
    }


    @Test
    void createCompletableTasksQuery_should_build_query_from_search_task_request_with_only_AND_queries()
        throws JsonProcessingException, JSONException {

        List<String> taskTypes = new ArrayList<>();
        taskTypes.add("Test Task Type");

        CamundaSearchQuery camundaSearchQuery = camundaQueryBuilder.createCompletableTasksQuery("caseId", taskTypes);

        String resultJson = objectMapper.writeValueAsString(camundaSearchQuery);
        String expected = "{\n"
                          + "  \"queries\": {\n"
                          + "    \"orQueries\": [\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"taskType\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"Test Task Type\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
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
                          + "      },\n"
                          + "      {\n"
                          + "        \"taskVariables\": [\n"
                          + "          {\n"
                          + "            \"name\": \"caseId\",\n"
                          + "            \"operator\": \"eq\",\n"
                          + "            \"value\": \"caseId\"\n"
                          + "          }\n"
                          + "        ]\n"
                          + "      }\n"
                          + "    ],\n"
                          + "    \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
                          + "  }\n"
                          + "}\n";

        JSONAssert.assertEquals(expected, resultJson, false);
    }

}
