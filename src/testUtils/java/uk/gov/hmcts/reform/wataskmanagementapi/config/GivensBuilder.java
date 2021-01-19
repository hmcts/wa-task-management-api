package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.RoleRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

public class GivensBuilder {

    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public GivensBuilder(RestApiActions camundaApiActions,
                         RestApiActions restApiActions,
                         AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;

    }

    public GivensBuilder iCreateATaskWithCustomVariables(Map<String, CamundaValue<?>> processVariables) {

        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables
        );

        Response result = camundaApiActions.post(
            "message",
            request,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return this;
    }

    public GivensBuilder iCreateATaskWithCaseId(String caseId,
                                                String tribunalCaseworkerPermissions) {

        Map<String, CamundaValue<?>> processVariables = createDefaultTaskVariables(
            caseId,
            tribunalCaseworkerPermissions
        );

        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables
        );

        Response result = camundaApiActions.post(
            "message",
            request,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return this;
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value) {

        String filter = "?processVariables=" + key + "_eq_" + value;

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Response result = camundaApiActions.get(
            "/task" + filter,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        return result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract()
            .jsonPath().getList("", CamundaTask.class);
    }

    public GivensBuilder and() {
        return this;
    }

    public void iClaimATaskWithIdAndAuthorization(String taskId, Headers headers) {
        Response result = restApiActions.post(
            "task/{task-id}/claim",
            taskId,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    public GivensBuilder iAddVariablesToTaskWithId(String taskId, CamundaProcessVariables processVariables) {
        Response result = camundaApiActions.post(
            "/task/{task-id}/variables",
            taskId,
            new Modifications(processVariables.getProcessVariablesMap()),
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return this;
    }

    public GivensBuilder iUpdateVariablesOfTaskById(String taskId, Map<String, CamundaValue<?>> processVariables) {
        Response result = camundaApiActions.post(
            "/task/{task-id}/variables",
            taskId,
            new Modifications(processVariables),
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return this;
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariables(String caseId,
                                                                   String tribunalCaseworkerPermissions) {
        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "IA")
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("region", "east-england")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "A Hearing Centre")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskState", "configured")
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("taskState", "configured")
            .withProcessVariable("dueDate", now().plusDays(2).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("tribunal-caseworker", tribunalCaseworkerPermissions)
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", ZonedDateTime.now().format(CAMUNDA_DATA_TIME_FORMATTER))
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
