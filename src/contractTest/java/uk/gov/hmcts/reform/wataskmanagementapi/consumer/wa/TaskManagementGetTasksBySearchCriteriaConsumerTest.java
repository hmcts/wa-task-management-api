package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_search", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class, EntityManager.class, EntityManagerFactory.class})
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskManagementGetTasksBySearchCriteriaConsumerTest extends SpringBootContractBaseTest {

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_SEARCH_QUERY = "/task";

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQuery200(PactDslWithProvider builder) {
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
    public RequestResponsePact executeSearchQueryWa200(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by criteria for jurisdiction type wa")
            .uponReceiving("Provider receives a POST /task request from a WA API for jurisdiction type wa")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseRequestForWa(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskForWa())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithWorkType200(PactDslWithProvider builder) {
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
    public RequestResponsePact executeSearchQueryWithWarnings200(PactDslWithProvider builder) {
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
    public RequestResponsePact executeSearchQueryWithWorkTypeWithWarnings200(PactDslWithProvider builder) {
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

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithRoleCategory200(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by criteria with role category")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchByRoleCategoryRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskForRoleCategory())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithTaskType200(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by criteria with task type")
            .uponReceiving("Provider receives a POST /task request from a WA API for task type")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchWithTaskTypeRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskForTaskType())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithAvailableTasksContext200Test(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by criteria with context available task")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseWithAvailableTasksContext(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskWithWarnings())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchQueryWithAllWorkContext200Test(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by criteria with context all work")
            .uponReceiving("Provider receives a POST /task request from a WA API")
            .path(WA_SEARCH_QUERY)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createSearchEventCaseWithAllWorkContext(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskWithWarnings())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQuery200", pactVersion = PactSpecVersion.V3)
    void testSearchQuery200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWa200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWa200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseRequestForWa())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWarnings200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithWarnings200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWorkType200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithWorkType200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithWorkTypeRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithWorkTypeWithWarnings200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithWorkTypeWithWarnings200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithWorkTypeRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithRoleCategory200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithRoleCategory200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchByRoleCategoryRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithTaskType200", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithTaskType200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchWithTaskTypeRequest())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithAvailableTasksContext200Test",
        pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithAvailableTasksContext200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithAvailableTasksContext())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }


    @Test
    @PactTestFor(pactMethod = "executeSearchQueryWithAllWorkContext200Test", pactVersion = PactSpecVersion.V3)
    void testSearchQueryWithAllWorkContext200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createSearchEventCaseWithAllWorkContext())
            .post(mockServer.getUrl() + WA_SEARCH_QUERY)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private DslPart createResponseForGetTask() {
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
                        .stringType("work_type_label", "Hearing work")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                )).build();
    }

    private DslPart createResponseForGetTaskForWa() {
        return newJsonBody(
            o -> o
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "4d4b6fgh-c91f-433f-92ac-e456ae34f72a")
                        .stringType("name", "Process Application")
                        .stringType("type", "processApplication")
                        .stringType("task_state", "unassigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "Process Application")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .booleanType("auto_assigned", false)
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "WA")
                        .stringType("region", "1")
                        .stringType("location", "765324")
                        .stringType("location_name", "Taylor House")
                        .stringType("case_type_id", "WaCaseType")
                        .stringType("case_id", "1617708245335311")
                        .stringType("case_category", "Protection")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("warnings", false)
                        .stringType("case_management_category", "Protection")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .object("additional_properties", value -> value
                            .stringType("name1", "value1")
                            .stringType("name2", "value2")
                            .stringType("name3", "value3")
                        )
                        .object("permissions", (value) -> {
                            value
                                .unorderedArray("values", (p) -> p
                                    .stringValue(PermissionTypes.READ.value())
                                    .stringValue(PermissionTypes.EXECUTE.value())
                                    .stringValue(PermissionTypes.REFER.value())
                                    .stringValue(PermissionTypes.COMPLETE.value())
                                    .stringValue(PermissionTypes.ASSIGN.value())
                                    .stringValue(PermissionTypes.UNASSIGN.value()));
                        })
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
                        .object("additional_properties", value -> value
                            .stringType("name1", "value1")
                            .stringType("name2", "value2")
                            .stringType("name3", "value3")
                        )
                        .stringType("case_management_category", "Some Case Management Category")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
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

        return """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;

    }

    private String createSearchEventCaseRequestForWa() {

        return """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "WA"
                    ]
                  }
                ]
              }
            """;

    }

    private String createSearchEventCaseWithWorkTypeRequest() {
        return """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  },
                  {
                    "key": "work_type",
                    "operator": "IN",
                    "values": [
                      "routine_work","decision_making_work","hearing_work","applications","upper_tribunal","priority",
                      "access_requests","error_management","review_case","evidence","follow_up"
                    ]
                  }
                ]
              }
            """;
    }

    private String createSearchEventCaseWithAvailableTasksContext() {

        return """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
    }

    private String createSearchEventCaseWithAllWorkContext() {
        return """
            {
                "request_context": "ALL_WORK",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
    }

    private String createSearchByRoleCategoryRequest() {
        return """
            {
                "search_parameters": [
                  {
                    "key": "role_category",
                    "operator": "IN",
                    "values": [
                      "CTSC"
                    ]
                  }
                ]
              }
            """;
    }

    private String createSearchWithTaskTypeRequest() {
        return """
            {
                "search_parameters": [
                  {
                    "key": "task_type",
                    "operator": "IN",
                    "values": [
                      "processApplication", "reviewAppealSkeletonArgument", "decideOnTimeExtension", "attendCma",
                      "followUpOverdueCaseBuilding", "reviewRespondentResponse", "followUpOverdueRespondentEvidence"
                    ]
                  }
                ]
              }
            """;
    }

    private DslPart createResponseForGetTaskForRoleCategory() {
        return newJsonBody(
            o -> o
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "4d4b6fgh-c91f-433f-92ac-e456ae34f72a")
                        .stringType("name", "review appeal skeleton argument")
                        .stringType("type", "reviewAppealSkeletonArgument")
                        .stringType("task_state", "unassigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "review appeal skeleton argument")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .stringType("assignee", "10bac6bf-80a7-4c81-b2db-516aba826be6")
                        .booleanType("auto_assigned", true)
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "WA")
                        .stringType("region", "1")
                        .stringType("location", "765324")
                        .stringType("location_name", "Taylor House")
                        .stringType("case_type_id", "WaCaseType")
                        .stringType("case_id", "1617708245335311")
                        .stringType("case_category", "Protection")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("warnings", false)
                        .stringType("case_management_category", "Some Case Management Category")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .stringValue("role_category", "CTSC")
                        .stringType("description", "aDescription")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                )).build();
    }

    private DslPart createResponseForGetTaskForTaskType() {
        return newJsonBody(
            o -> o
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "b1a13dca-41a5-424f-b101-c67b439549d0")
                        .stringType("name", "review appeal skeleton argument")
                        .stringType("type", "reviewAppealSkeletonArgument")
                        .stringType("task_state", "assigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "review appeal skeleton argument")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .stringType("assignee", "10bac6bf-80a7-4c81-b2db-516aba826be6")
                        .booleanType("auto_assigned", true)
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "WA")
                        .stringType("region", "1")
                        .stringType("location", "765324")
                        .stringType("location_name", "Taylor House")
                        .stringType("case_type_id", "WaCaseType")
                        .stringType("case_id", "1617708245335399")
                        .stringType("case_category", "Protection")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("warnings", false)
                        .stringType("case_management_category", "Some Case Management Category")
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .stringValue("role_category", "LEGAL_OPERATIONS")
                        .stringType("description", "aDescription")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                )).build();
    }
}
