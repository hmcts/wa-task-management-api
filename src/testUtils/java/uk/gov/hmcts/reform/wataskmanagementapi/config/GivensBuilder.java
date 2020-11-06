package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

public class GivensBuilder {

    private final String camundaUrl;
    private final CamundaObjectMapper camundaObjectMapper;
    private final IdamServiceApi idamServiceApi;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public GivensBuilder(String camundaUrl,
                         CamundaObjectMapper camundaObjectMapper,
                         IdamServiceApi idamServiceApi,
                         RoleAssignmentServiceApi roleAssignmentServiceApi,
                         AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaUrl = camundaUrl;
        this.camundaObjectMapper = camundaObjectMapper;
        this.idamServiceApi = idamServiceApi;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
        this.authorizationHeadersProvider = authorizationHeadersProvider;

    }

    public GivensBuilder iCreateATaskWithCustomVariables(Map<String, CamundaValue<?>> processVariables) {

        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables
        );

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .body(camundaObjectMapper.asCamelCasedJsonString(request))
            .when()
            .post("/message")
            .then()
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        return this;
    }

    public GivensBuilder iCreateATaskWithCaseId(String caseId) {

        Map<String, CamundaValue<?>> processVariables = createDefaultTaskVariables(caseId);

        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables
        );

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .header(authorizationHeadersProvider.getServiceAuthorizationHeader())
            .body(camundaObjectMapper.asCamelCasedJsonString(request))
            .when()
            .post("/message")
            .then()
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        return this;
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value) {

        String filter = "?processVariables=" + key + "_eq_" + value;

        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .header(authorizationHeadersProvider.getServiceAuthorizationHeader())
            .when()
            .get("/task" + filter)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract()
            .jsonPath().getList("", CamundaTask.class);
    }

    public GivensBuilder and() {
        return this;
    }


    public void iClaimATaskWithIdAndAuthorization(String taskId, Headers headers) {
        Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(headers)
            .when()
            .post("task/{task-id}/claim", taskId);

        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    public GivensBuilder iAddVariablesToTaskWithId(String taskId, CamundaProcessVariables processVariables) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .header(authorizationHeadersProvider.getServiceAuthorizationHeader())
            .body(new Modifications(processVariables.getProcessVariablesMap()))
            .when()
            .post("/task/{task-id}/variables", taskId)
            .then()
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        return this;
    }

    public void iAllocateACaseToUserAs(Headers headers, String roleName, String caseId) {

        String userId = idamServiceApi.userInfo(headers.getValue(AUTHORIZATION)).getUid();

        RoleAssignmentRequest roleAssignmentRequest = createRoleAssignmentRequest(userId, roleName, caseId);

        roleAssignmentServiceApi.createRoleAssignment(
            camundaObjectMapper.asCamelCasedJsonString(roleAssignmentRequest),
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION)
        );
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariables(String caseId) {
        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "IA")
            .withProcessVariable("ccdId", caseId)
            .withProcessVariable("region", "east-england")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "A Hearing Centre")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("dueDate", now().plusDays(2).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .build();

        return processVariables.getProcessVariablesMap();
    }

    private RoleAssignmentRequest createRoleAssignmentRequest(String userId, String roleName, String caseId) {
        String process = "case-allocation";
        String reference = caseId + "/" + roleName;
        RoleRequest roleRequest = new RoleRequest(userId, process, reference, true);
        Map<String, String> attributes = Map.of(
            "caseId", caseId
        );
        Assignment assignment = new Assignment(
            ActorIdType.IDAM,
            userId,
            RoleType.CASE,
            roleName,
            Classification.RESTRICTED,
            GrantType.SPECIFIC,
            RoleCategory.STAFF,
            false,
            attributes
        );

        return new RoleAssignmentRequest(
            roleRequest,
            singletonList(assignment)
        );
    }

    private class Modifications {
        private final Map<String, CamundaValue<?>> modifications;

        public Modifications(Map<String, CamundaValue<?>> processVariablesMap) {
            super();
            this.modifications = processVariablesMap;
        }

        public Map<String, CamundaValue<?>> getModifications() {
            return modifications;
        }
    }
}
