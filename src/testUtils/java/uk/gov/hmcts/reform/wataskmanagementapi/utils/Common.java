package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType.CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType.ORGANISATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Slf4j
public class Common {

    public static final String REASON_COMPLETED = "completed";
    public static final String REASON_DELETED = "deleted";
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    private static final String ENDPOINT_HISTORY_TASK = "history/task";
    private final GivensBuilder given;
    private final RestApiActions camundaApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    private final IdamWebApi idamWebApi;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Common(GivensBuilder given,
                  RestApiActions camundaApiActions,
                  AuthorizationHeadersProvider authorizationHeadersProvider,
                  IdamWebApi idamWebApi,
                  RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.given = given;
        this.camundaApiActions = camundaApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
        this.idamWebApi = idamWebApi;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public TestVariables setupTaskAndRetrieveIdsWithCustomVariablesOverride(
        Map<CamundaVariableDefinition, String> variablesToUseAsOverride
    ) {
        String caseId = given.iCreateACcdCase();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId);

        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesToUseAsOverride.get(key), "String")));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.isEmpty()) {
            fail("Search did not yield any results for case id: " + caseId);
        }

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());

    }

    public void updateTaskWithCustomVariablesOverride(TestVariables task,
                                                      Map<CamundaVariableDefinition, String> variablesToUseAsOverride

    ) {
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(task.getCaseId());
        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesToUseAsOverride.get(key), "String")));

        given.iUpdateVariablesOfTaskById(task.getTaskId(), processVariables);
    }


    public void overrideTaskPermissions(String taskId, String permissions) {
        given.iUpdateTaskVariable(
            taskId,
            Map.of("tribunal-caseworker", new CamundaValue<>(permissions, "String"))
        );
    }

    public TestVariables setupTaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition key, String value) {
        String caseId = given.iCreateACcdCase();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId);
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskWithoutCcdCaseAndRetrieveIdsWithCustomVariable(
        CamundaVariableDefinition key, String value
    ) {
        final String caseId = UUID.randomUUID().toString();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId);
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskAndRetrieveIds() {

        String caseId = given.iCreateACcdCase();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public void cleanUpTask(String taskId, String reason) {
        log.info("Cleaning task {}", taskId);
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

    public void clearAllRoleAssignments(Headers headers) {
        UserInfo userInfo = idamWebApi.userInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
    }

    public void setupOrganisationalRoleAssignment(Headers headers) {

        UserInfo userInfo = idamWebApi.userInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", "IA"
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );
    }

    private String toJsonString(Map<String, String> attributes) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    public void setupOrganisationalRoleAssignmentWithCustomAttributes(Headers headers, Map<String, String> attributes) {

        UserInfo userInfo = idamWebApi.userInfo(headers.getValue(AUTHORIZATION));

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );
    }

    public void setupRestrictedRoleAssignment(String caseId, Headers headers) {

        UserInfo userInfo = idamWebApi.userInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "jurisdiction", "IA",
            "primaryLocation", "765324"
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );

        //Creates a restricted role for a particular ccdId
        log.info("Creating Restricted role-assignment");
        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            null,
            "requests/roleAssignment/set-restricted-role-assignment-request.json"
        );
    }

    private void postRoleAssignment(String caseId,
                                    String bearerUserToken,
                                    String s2sToken,
                                    UserInfo userInfo,
                                    String attributes,
                                    String resourceFilename) {

        try {
            roleAssignmentServiceApi.createRoleAssignment(
                getBody(caseId, userInfo, resourceFilename, attributes),
                bearerUserToken,
                s2sToken
            );
        } catch (FeignException ex) {
            ex.printStackTrace();
        }
    }

    private void clearAllRoleAssignmentsForUser(String userId, Headers headers) {
        String userToken = headers.getValue(AUTHORIZATION);
        String serviceToken = headers.getValue(SERVICE_AUTHORIZATION);

        GetRoleAssignmentResponse response = null;

        try {
            //Retrieve All role assignments
            response = roleAssignmentServiceApi.getRolesForUser(userId, userToken, serviceToken);

        } catch (FeignException ex) {
            if (ex.status() == HttpStatus.NOT_FOUND.value()) {
                System.out.println("No roles found, nothing to delete.");
            } else {
                ex.printStackTrace();
            }
        }

        if (response != null) {
            //Delete All role assignments
            List<Assignment> organisationalRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> ORGANISATION.equals(assignment.getRoleType()))
                .collect(toList());

            List<Assignment> caseRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> CASE.equals(assignment.getRoleType()))
                .collect(toList());

            //Check if there are 'orphaned' restricted roles
            if (organisationalRoleAssignments.isEmpty() && !caseRoleAssignments.isEmpty()) {
                log.info("Orphaned Restricted role assignments were found.");
                log.info("Creating a temporary role assignment to perform cleanup");
                //Create a temporary organisational role
                setupOrganisationalRoleAssignment(headers);
                //Recursive
                clearAllRoleAssignments(headers);
            }

            caseRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken));

            organisationalRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken));
        }
    }

    private String getBody(final String caseId,
                           final UserInfo userInfo,
                           final String resourceFilename,
                           final String attributes) {
        String assignmentRequestBody = null;
        try {
            assignmentRequestBody = FileUtils.readFileToString(ResourceUtils.getFile("classpath:" + resourceFilename));
            assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", userInfo.getUid());
            assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", userInfo.getUid());
            if (attributes != null) {
                assignmentRequestBody = assignmentRequestBody.replace("\"{ATTRIBUTES_PLACEHOLDER}\"", attributes);
            }
            if (caseId != null) {
                assignmentRequestBody = assignmentRequestBody.replace("{CASE_ID_PLACEHOLDER}", caseId);

            }

            return assignmentRequestBody;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assignmentRequestBody;
    }

}
