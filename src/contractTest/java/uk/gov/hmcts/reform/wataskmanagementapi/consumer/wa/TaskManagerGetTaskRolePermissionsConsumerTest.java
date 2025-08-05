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
import jakarta.persistence.EntityManagerFactory;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_task_role_permissions_by_task_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskManagerGetTaskRolePermissionsConsumerTest extends SpringBootContractBaseTest {

    @MockitoBean
    EntityManagerFactory entityManagerFactory;

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_GET_TASK_ROLE_PERMISSIONS_BY_ID = WA_URL + "/" + TASK_ID + "/" + "roles";

    @Pact(provider = "wa_task_management_api_task_role_permissions_by_task_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetTaskRolePermissionsById200(PactDslWithProvider builder) {
        return builder
            .given("get task role information using taskId")
            .uponReceiving("taskId to get task role permissions")
            .path(WA_GET_TASK_ROLE_PERMISSIONS_BY_ID)
            .method(HttpMethod.GET.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetTaskRolePermissionsById200", pactVersion = PactSpecVersion.V3)
    void testGetTaskRolePermissionsByTaskId200Test(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .get(mockServer.getUrl() + WA_GET_TASK_ROLE_PERMISSIONS_BY_ID)
            .then()
            .statusCode(200);

    }

    private DslPart createResponseForGetTask() {
        return newJsonBody(
            o -> o
                .minArrayLike("roles", 1, 1,
                    role -> role
                        .stringType("role_category", "LEGAL_OPERATIONS")
                        .stringType("role_name", "tribunal-caseworker")
                        .array("permissions",
                            permission -> permission
                                .stringType(PermissionTypes.READ.value())
                                .stringType(PermissionTypes.OWN.value())
                                .stringType(PermissionTypes.MANAGE.value())
                                .stringType(PermissionTypes.EXECUTE.value())
                                .stringType(PermissionTypes.CANCEL.value())
                                .stringType(PermissionTypes.COMPLETE.value())
                                .stringType(PermissionTypes.COMPLETE_OWN.value())
                                .stringType(PermissionTypes.CANCEL_OWN.value())
                                .stringType(PermissionTypes.UNCLAIM.value())
                                .stringType(PermissionTypes.ASSIGN.value())
                                .stringType(PermissionTypes.UNASSIGN.value())
                                .stringType(PermissionTypes.UNCLAIM_ASSIGN.value())
                                .stringType(PermissionTypes.UNASSIGN_CLAIM.value())
                                .stringType(PermissionTypes.UNASSIGN_ASSIGN.value()))
                        .array("authorisations",
                            authorisation -> authorisation
                                .stringType("373")
                                .stringType("SCSS"))
                )).build();
    }

    private Map<String, String> getTaskManagementServiceResponseHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .put(AUTHORIZATION, AUTH_TOKEN)
            .build();
    }
}
