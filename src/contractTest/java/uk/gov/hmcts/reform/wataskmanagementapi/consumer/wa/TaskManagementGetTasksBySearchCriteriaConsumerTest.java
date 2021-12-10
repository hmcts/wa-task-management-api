package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_search", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskManagementGetTasksBySearchCriteriaConsumerTest extends SpringBootContractBaseTest {

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_SEARCH_QUERY = "/task";
    //private static final String CONTENT_TYPE = "application/json\r\n";

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQuery200(PactDslWithProvider builder) throws JsonProcessingException {
        return builder
            .given("appropriate tasks are returned by criteria")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact testSearchQueryWithAvailableTasksOnly200(PactDslWithProvider builder) throws
        JsonProcessingException {
        return builder
            .given("appropriate tasks are returned by criteria with available tasks only")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseWithAvailableTasks(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithWorkType200(PactDslWithProvider builder)
        throws JsonProcessingException {
        return builder
            .given("appropriate tasks are returned by criteria with work-type")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseWithWorkTypeRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithWarnings200(PactDslWithProvider builder)
        throws JsonProcessingException {
        return builder
            .given("appropriate tasks are returned by criteria with warnings only")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskWithWarnings())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithWorkTypeWithWarnings200(PactDslWithProvider builder)
        throws JsonProcessingException {
        return builder
            .given("appropriate tasks are returned by criteria with work-type with warnings only")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseWithWorkTypeRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskWithWarnings())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQuery200")
    void testSearchQuery200Test(MockServer mockServer) {
        SerenityRest.registerParser("application/json", Parser.JSON);
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "testSearchQueryWithAvailableTasksOnly200")
    void testSearchQueryWithAvailableTasksOnly200Test(MockServer mockServer) {
        SerenityRest.registerParser("application/json", Parser.JSON);
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithAvailableTasks())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWorkType200")
    void testSearchQueryWithWorkType200Test(MockServer mockServer) {
        SerenityRest.registerParser("application/json", Parser.JSON);
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithWorkTypeRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWarnings200")
    void testSearchQueryWithWarnings200Test(MockServer mockServer) {
        SerenityRest.registerParser("application/json", Parser.JSON);
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWorkTypeWithWarnings200")
    void testSearchQueryWithWorkTypeWithWarnings200Test(MockServer mockServer) {
        SerenityRest.registerParser("application/json", Parser.JSON);
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithWorkTypeRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .contentType(ContentType.JSON)
            .statusCode(HttpStatus.OK.value());
    }

    private DslPart createResponseForGetTask() throws JsonProcessingException {
        return newJsonBody(
            o -> o
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "4d4b6fgh-c91f-433f-92ac-e456ae34f72a")
                        .stringType("name", "Review the appeal")
                        .stringType("type", "ReviewTheAppeal")
                        .stringType("task_state", "assigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "Review the appeal")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .stringType("assignee", "10bac6bf-80a7-4c81-b2db-516aba826be6")
                        .booleanType("auto_assigned", true)
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "IA")
                        .stringType("region", "1")
                        .stringType("location", "765324")
                        .stringType("location_name", "Taylor House")
                        .stringType("case_type_id", "Asylum")
                        .stringType("case_id", "1617708245335311")
                        .stringType("case_category", "refusalOfHumanRights")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("warnings", false)
                        .stringType("case_management_category", "Some Case Management Category")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                )).build();
    }

    private DslPart createResponseForGetTaskWithWarnings() {
        return newJsonBody(
            o -> o
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "fda422de-b381-43ff-94ea-eea5790188a3")
                        .stringType("name", "Review the appeal")
                        .stringType("type", "ReviewTheAppeal")
                        .stringType("task_state", "assigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "Review the appeal")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .booleanType("auto_assigned", true)
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "IA")
                        .stringType("region", "1")
                        .stringType("location", "765324")
                        .stringType("location_name", "Taylor House")
                        .stringType("case_type_id", "Asylum")
                        .stringType("case_id", "1617708245308495")
                        .stringType("case_category", "refusalOfHumanRights")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("warnings", true)
                        .object("warning_list", values -> values
                            .minArrayLike("values", 1, value -> value
                                .stringType("warningCode", "Code1")
                                .stringType("warningText", "Text1")
                            )
                        )
                        .stringType("case_management_category", "Some Case Management Category")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                )).build();
    }

    private Map<String, String> getTaskManagementServiceResponseHeaders() {

        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .put(AUTHORIZATION, AUTH_TOKEN)
            .build();
    }

    private String createSearchEventCaseRequest() {
        String request = "";
        request = "{\n"
                  + "    \"search_parameters\": [\n"
                  + "         {\n"
                  + "             \"key\": \"jurisdiction\",\n"
                  + "             \"operator\": \"IN\",\n"
                  + "             \"values\":"
                  + "                 [\n"
                  + "                     \"IA\"\n"
                  + "                 ]\n"
                  + "          }\n"
                  + "      ]\n"
                  + "  }\n";
        return request;
    }

    private String createSearchEventCaseWithWorkTypeRequest() {

        return "{\n"
               + "    \"search_parameters\": [\n"
               + "        {\n"
               + "            \"key\": \"jurisdiction\",\n"
               + "            \"operator\": \"IN\",\n"
               + "            \"values\": [\n"
               + "                \"IA\"\n"
               + "            ]\n"
               + "        },\n"
               + "        {\n"
               + "            \"key\": \"work_type\",\n"
               + "            \"operator\": \"IN\",\n"
               + "            \"values\": [\n"
               + "                \"routine_work\",\n"
               + "                \"decision_making_work\",\n"
               + "                \"hearing_work\",\n"
               + "                \"applications\",\n"
               + "                \"upper_tribunal\",\n"
               + "                \"priority\",\n"
               + "                \"error_management\",\n"
               + "                \"access_requests\"\n"
               + "            ]\n"
               + "        }\n"
               + "    ]\n"
               + "}";
    }

    private String createSearchEventCaseWithAvailableTasks() {

        return "{\n"
               + "    \"search_parameters\": [\n"
               + "        {\n"
               + "            \"key\": \"jurisdiction\",\n"
               + "            \"operator\": \"IN\",\n"
               + "            \"values\": [\n"
               + "                \"IA\"\n"
               + "            ]\n"
               + "        },\n"
               + "        {\n"
               + "            \"key\": \"available_tasks_only\",\n"
               + "            \"operator\": \"BOOLEAN\",\n"
               + "            \"value\": true"
               + "        }\n"
               + "    ]\n"
               + "}";
    }

}
