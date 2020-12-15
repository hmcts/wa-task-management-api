package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseIdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class Common {

    public static final String TRIBUNAL_CASEWORKER_PERMISSIONS = "Read,Refer,Own,Manage,Cancel";
    public static final String REASON_COMPLETED = "completed";
    public static final String REASON_DELETED = "deleted";
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    private static final String ENDPOINT_HISTORY_TASK = "history/task";
    private final CaseIdGenerator caseIdGenerator;
    private final GivensBuilder given;
    private final RestApiActions camundaApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public Common(CaseIdGenerator caseIdGenerator, GivensBuilder given,
                  RestApiActions camundaApiActions, AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.caseIdGenerator = caseIdGenerator;
        this.given = given;
        this.camundaApiActions = camundaApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
    }

    public Map<String, String> setupTaskAndRetrieveIdsWithCustomVariablesOverride(
        Map<CamundaVariableDefinition, String> variablesToUseAsOverride
    ) {
        String caseId = caseIdGenerator.generate();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            caseId,
            TRIBUNAL_CASEWORKER_PERMISSIONS
        );

        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesToUseAsOverride.get(key), "String")));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return Map.of(
            "caseId", caseId,
            "taskId", response.get(0).getId()
        );

    }

    public void updateTaskWithCustomVariablesOverride(Map<String,String> task,
        Map<CamundaVariableDefinition, String> variablesToUseAsOverride

    ) {
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            task.get("caseId"),
            TRIBUNAL_CASEWORKER_PERMISSIONS
        );
        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesToUseAsOverride.get(key), "String")));

        given.iUpdateVariablesOfTaskById(task.get("taskId"),processVariables);
    }

    public Map<String, String> setupTaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition key, String value) {
        String caseId = caseIdGenerator.generate();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            caseId,
            TRIBUNAL_CASEWORKER_PERMISSIONS
        );
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return Map.of(
            "caseId", caseId,
            "taskId", response.get(0).getId()
        );

    }

    public Map<String, String> setupTaskAndRetrieveIds(String tribunalCaseworkerPermissions) {
        String caseId = caseIdGenerator.generate();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, tribunalCaseworkerPermissions)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        new HashMap<>();

        return Map.of(
            "caseId", caseId,
            "taskId", response.get(0).getId()
        );

    }

    public void cleanUpTask(String taskId, String reason) {
        camundaApiActions.post(ENDPOINT_COMPLETE_TASK, taskId,
                               authorizationHeadersProvider.getServiceAuthorizationHeadersOnly());

        Response result = camundaApiActions.get(
            ENDPOINT_HISTORY_TASK + "?taskId=" + taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("[0].deleteReason", is(reason));
    }

}
