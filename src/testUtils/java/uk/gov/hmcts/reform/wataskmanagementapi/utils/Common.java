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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType.CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType.ORGANISATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WARNINGS;

@Slf4j
public class Common {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private final GivensBuilder given;
    private final RestApiActions restApiActions;
    private final RestApiActions camundaApiActions;
    private final AuthorizationProvider authorizationProvider;

    private final IdamService idamService;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Common(GivensBuilder given,
                  RestApiActions restApiActions,
                  RestApiActions camundaApiActions,
                  AuthorizationProvider authorizationProvider,
                  IdamService idamService,
                  RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.given = given;
        this.restApiActions = restApiActions;
        this.camundaApiActions = camundaApiActions;
        this.authorizationProvider = authorizationProvider;
        this.idamService = idamService;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public TestVariables setupTaskAndRetrieveIdsWithCustomVariablesOverride(
        Map<CamundaVariableDefinition, String> variablesToUseAsOverride, String jurisdiction, String caseType
    ) {
        String caseId = given.iCreateACcdCase();
        Map<String, CamundaValue<?>> processVariables
            = given.createDefaultTaskVariables(caseId, jurisdiction, caseType);

        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables.put(
                key.value(),
                new CamundaValue<>(variablesToUseAsOverride.get(key), "String")
            ));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

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
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            task.getCaseId(),
            "IA",
            "Asylum"
        );
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
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId,
                                                                                         "IA",
                                                                                         "Asylum"
        );
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskWithoutCcdCaseAndRetrieveIdsWithCustomVariable(
        CamundaVariableDefinition key, String value
    ) {
        final String caseId = UUID.randomUUID().toString();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId,
                                                                                         "IA",
                                                                                         "Asylum"
        );
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskAndRetrieveIds() {

        String caseId = given.iCreateACcdCase();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, false, "IA", "Asylum")
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskAndRetrieveIds(String taskType) {

        String caseId = given.iCreateACcdCase();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, taskType)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public List<CamundaTask> setupTaskAndRetrieveIdsForGivenCaseId(String caseId, String taskType) {
        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, taskType)
            .and()
            .iRetrieveATasksWithProcessVariableFilter("caseId", caseId, taskType);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }
        // return taskId
        return response;
    }

    public TestVariables setupWATaskAndRetrieveIds() {

        String caseId = given.iCreateWACcdCase();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, false, "WA", "WaCaseType")
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public TestVariables setupTaskWithWarningsAndRetrieveIds() {

        String caseId = given.iCreateACcdCase();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, true, "IA", "Asylum")
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId());
    }

    public void cleanUpTask(String... taskId) {
        Stream.of(taskId).forEach(task -> {
            log.info("Cleaning task {}", task);
            camundaApiActions.post(ENDPOINT_COMPLETE_TASK, task,
                authorizationProvider.getServiceAuthorizationHeadersOnly());
        });
    }

    public void cleanUpAndValidateCftTaskState(String taskId, String reason) {
        log.info("Cleaning task {}", taskId);
        Response response = camundaApiActions.post(ENDPOINT_COMPLETE_TASK, taskId,
            authorizationProvider.getServiceAuthorizationHeadersOnly());

        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value())
            .body("cftTaskState.value", is(reason));
    }

    public Response getCamundaTask(String taskId) {
        return camundaApiActions.get("/task", taskId,
            authorizationProvider.getServiceAuthorizationHeadersOnly());
    }

    public void clearAllRoleAssignments(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
    }

    public void setupOrganisationalRoleAssignment(Headers headers, String roleName) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));

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
            roleName,
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );
    }

    public void setupOrganisationalRoleAssignment(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );

    }

    public void setupCFTOrganisationalRoleAssignment(Headers headers, String roleName, String jurisdictionId) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", jurisdictionId
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
            roleName,
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );
    }

    public void setupCFTOrganisationalRoleAssignment(Headers headers, String jurisdictionId) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", jurisdictionId
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

    }

    public void setupCFTOrganisationalRoleAssignmentForWA(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324"
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

    }

    public void setupCFTOrganisationalWithMultipleRoles(Headers headers, String jurisdictionId) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", jurisdictionId
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

    }

    public void setupOrganisationalRoleAssignmentWithWorkTypes(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", "IA",
            "workTypes","hearing_work,upper_tribunal,routine_work"
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );
    }

    public void setupOrganisationalRoleAssignmentWithOutEndDate(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request-without-end-date.json"
        );
    }

    public void setupOrganisationalRoleAssignmentWithCustomAttributes(Headers headers, Map<String, String> attributes) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request.json"
        );
    }

    public void setupRestrictedRoleAssignment(String caseId, Headers headers) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));

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
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json"
        );

        //Creates a restricted role for a particular ccdId
        log.info("Creating Restricted role-assignment");
        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo,
            "tribunal-caseworker",
            null,
            "requests/roleAssignment/set-restricted-role-assignment-request.json"
        );
    }

    public void insertTaskInCftTaskDb(TestVariables testVariables, String taskType, Headers authenticationHeaders) {
        String warnings = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                          + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CASE_CATEGORY, "Protection"),
            new TaskAttribute(TASK_ROLE_CATEGORY, "LEGAL_OPERATIONS"),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_WARNINGS, warnings),
            new TaskAttribute(TASK_AUTO_ASSIGNED, true),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
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

    private void postRoleAssignment(String caseId,
                                    String bearerUserToken,
                                    String s2sToken,
                                    UserInfo userInfo,
                                    String roleName,
                                    String attributes,
                                    String resourceFilename) {

        try {
            roleAssignmentServiceApi.createRoleAssignment(
                getBody(caseId, userInfo, roleName, resourceFilename, attributes),
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

        RoleAssignmentResource response = null;

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
            List<RoleAssignment> organisationalRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> ORGANISATION.equals(assignment.getRoleType()))
                .collect(toList());

            List<RoleAssignment> caseRoleAssignments = response.getRoleAssignmentResponse().stream()
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
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );

            organisationalRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );
        }
    }

    private String getBody(final String caseId,
                           final UserInfo userInfo,
                           final String roleName,
                           final String resourceFilename,
                           final String attributes) {
        String assignmentRequestBody = null;
        try {
            assignmentRequestBody = FileUtils.readFileToString(ResourceUtils.getFile(
                "classpath:" + resourceFilename), "UTF-8"
            );
            assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", userInfo.getUid());
            assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", userInfo.getUid());
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_NAME_PLACEHOLDER}", roleName);
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
