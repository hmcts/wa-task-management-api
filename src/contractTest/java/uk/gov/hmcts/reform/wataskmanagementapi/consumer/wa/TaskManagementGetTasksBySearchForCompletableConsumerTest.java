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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_search_completable", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class, EntityManager.class, EntityManagerFactory.class})
@Import({TaskManagementProviderTestConfiguration.class})
public class TaskManagementGetTasksBySearchForCompletableConsumerTest extends SpringBootContractBaseTest {

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_URL = "/task";
    private static final String WA_SEARCH_FOR_COMPLETABLE = WA_URL + "/search-for-completable";

    @Pact(provider = "wa_task_management_api_search_completable", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchForCompletable200(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by search for completable")
            .uponReceiving("Provider receives a POST /task/search-for-completable request from a WA API")
            .path(WA_SEARCH_FOR_COMPLETABLE)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(creteSearchEventCaseRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search_completable", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchForCompletableWa200(PactDslWithProvider builder) {
        return builder
            .given("appropriate wa tasks are returned by search for completable")
            .uponReceiving("Provider receives a POST /task/search-for-completable request from a WA API")
            .path(WA_SEARCH_FOR_COMPLETABLE)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(creteSearchEventCaseRequestWa(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetWaTask())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_search_completable", consumer = "wa_task_management_api")
    public RequestResponsePact executeSearchForCompletableWithWarnings200(PactDslWithProvider builder) {
        return builder
            .given("appropriate tasks are returned by search for completable with warnings")
            .uponReceiving("Provider receives a POST /task/search-for-completable request from a WA API")
            .path(WA_SEARCH_FOR_COMPLETABLE)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(creteSearchEventCaseRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskWithWarnings())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchForCompletable200", pactVersion = PactSpecVersion.V3)
    void testSearchForCompletable200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(creteSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_FOR_COMPLETABLE)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchForCompletableWa200", pactVersion = PactSpecVersion.V3)
    void testSearchForCompletable200WaTest(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(creteSearchEventCaseRequestWa())
            .post(mockServer.getUrl() + WA_SEARCH_FOR_COMPLETABLE)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @PactTestFor(pactMethod = "executeSearchForCompletableWithWarnings200", pactVersion = PactSpecVersion.V3)
    void testSearchForCompletableWithWarnings200Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(creteSearchEventCaseRequest())
            .post(mockServer.getUrl() + WA_SEARCH_FOR_COMPLETABLE)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private DslPart createResponseForGetTask() {
        return newJsonBody(
            o -> o
                .booleanType("task_required_for_event", false)
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "4d4b6fgh-c91f-433f-92ac-e456ae34f72a")
                        .stringType("name", "Review the appeal")
                        .stringType("assignee", "10bac6bf-80a7-4c81-b2db-516aba826be6")
                        .stringType("type", "ReviewTheAppeal")
                        .stringType("task_state", "assigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "Review the appeal")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .stringType("location_name", "Taylor House")
                        .stringType("location", "765324")
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "IA")
                        .stringType("region", "1")
                        .stringType("case_type_id", "Asylum")
                        .stringType("case_id", "1617708245335311")
                        .stringType("case_category", "refusalOfHumanRights")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("auto_assigned", true)
                        .booleanType("warnings", false)
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .object("permissions", (value) -> {
                            value
                                .unorderedArray("values", (p) -> p
                                    .stringValue(PermissionTypes.READ.value())
                                    .stringValue(PermissionTypes.OWN.value())
                                    .stringValue(PermissionTypes.EXECUTE.value())
                                    .stringValue(PermissionTypes.CANCEL.value())
                                    .stringValue(PermissionTypes.MANAGE.value())
                                    .stringValue(PermissionTypes.REFER.value()));
                        })
                        .stringType("role_category", RoleCategory.LEGAL_OPERATIONS.name())
                        .stringType("description", "a description")
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                )).build();
    }

    private DslPart createResponseForGetWaTask() {
        return newJsonBody(
            o -> o
                .booleanType("task_required_for_event", false)
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
                        .stringType("description", "a description")
                        .stringType("role_category", RoleCategory.LEGAL_OPERATIONS.name())
                        .object("additional_properties", value -> value
                            .stringType("name1", "value1")
                            .stringType("name2", "value2")
                            .stringType("name3", "value3")
                        )
                        .object("permissions", (value) -> {
                            value.unorderedArray("values", (p) -> p
                                .stringValue(PermissionTypes.READ.value())
                                .stringValue(PermissionTypes.EXECUTE.value())
                                .stringValue(PermissionTypes.REFER.value()));
                        })
                        .stringType("next_hearing_id", "nextHearingId")
                        .datetime("next_hearing_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                )
        ).build();
    }

    private DslPart createResponseForGetTaskWithWarnings() {
        return newJsonBody(
            o -> o
                .booleanType("task_required_for_event", false)
                .minArrayLike("tasks", 1, 1,
                    task -> task
                        .stringType("id", "4d4b6fgh-c91f-433f-92ac-e456ae34f72a")
                        .stringType("name", "Review the appeal")
                        .stringType("assignee", "10bac6bf-80a7-4c81-b2db-516aba826be6")
                        .stringType("type", "ReviewTheAppeal")
                        .stringType("task_state", "assigned")
                        .stringType("task_system", "SELF")
                        .stringType("security_classification", "PUBLIC")
                        .stringType("task_title", "Review the appeal")
                        .datetime("due_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .datetime("created_date", "yyyy-MM-dd'T'HH:mm:ssZ")
                        .stringType("location_name", "Taylor House")
                        .stringType("location", "765324")
                        .stringType("execution_type", "Case Management Task")
                        .stringType("jurisdiction", "IA")
                        .stringType("region", "1")
                        .stringType("case_type_id", "Asylum")
                        .stringType("case_id", "1617708245335311")
                        .stringType("case_category", "refusalOfHumanRights")
                        .stringType("case_name", "Bob Smith")
                        .booleanType("auto_assigned", true)
                        .booleanType("warnings", true)
                        .stringType("work_type_id", "hearing_work")
                        .stringType("work_type_label", "Hearing work")
                        .object("warning_list", values -> values
                            .minArrayLike("values", 1, value -> value
                                .stringType("warningCode", "Code1")
                                .stringType("warningText", "Text1")
                            )
                        )
                        .object("permissions", (value) -> {
                            value
                                .unorderedArray("values", (p) -> p
                                    .stringValue(PermissionTypes.READ.value())
                                    .stringValue(PermissionTypes.OWN.value())
                                    .stringValue(PermissionTypes.EXECUTE.value())
                                    .stringValue(PermissionTypes.CANCEL.value())
                                    .stringValue(PermissionTypes.MANAGE.value())
                                    .stringValue(PermissionTypes.REFER.value()));
                        })
                        .object("additional_properties", value -> value
                            .stringType("name1", "value1")
                            .stringType("name2", "value2")
                            .stringType("name3", "value3")
                        )
                        .stringType("role_category", RoleCategory.LEGAL_OPERATIONS.name())
                        .stringType("description", "a description")
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

    private String creteSearchEventCaseRequest() {

        return "{\n"
               + "    \"case_id\": \"14a21569-eb80-4681-b62c-6ae2ed069e4f\",\n"
               + "    \"event_id\": \"requestRespondentEvidence\",\n"
               + "    \"case_jurisdiction\": \"IA\",\n"
               + "    \"case_type\": \"Asylum\",\n"
               + "  }\n";

    }

    private String creteSearchEventCaseRequestWa() {

        return "{\n"
               + "    \"case_id\": \"14a21569-eb80-4681-b62c-6ae2ed069e4f\",\n"
               + "    \"event_id\": \"requestRespondentEvidence\",\n"
               + "    \"case_jurisdiction\": \"WA\",\n"
               + "    \"case_type\": \"WaCaseType\",\n"
               + "  }\n";

    }
}
