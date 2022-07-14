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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;
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

@SuppressWarnings("checkstyle:LineLength")
@Slf4j
public class Common {

    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final DateTimeFormatter ROLE_ASSIGNMENT_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    public static final String R2_ROLE_ASSIGNMENT_REQUEST = "requests/roleAssignment/r2/set-organisational-role-assignment-request.json";
    public static final String R1_ROLE_ASSIGNMENT_REQUEST = "requests/roleAssignment/set-organisational-role-assignment-request.json";
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

    public TestVariables setupWATaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition key, String value, String resourceFileName) {
        String caseId = given.iCreateWACcdCase(resourceFileName);
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId,
                                                                                         "WA",
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

    public TestVariables setupWATaskAndRetrieveIds(String resourceFileName) {

        String caseId = given.iCreateWACcdCase(resourceFileName);

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

    public void clearAllRoleAssignmentsForChallenged(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUserChallenged(userInfo.getUid(), headers);
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
            userInfo.getUid(),
            roleName,
            toJsonString(attributes),
            R1_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid());
    }

    public void setupOrganisationalRoleAssignment(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));//Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, "IA");
        createStandardTribunalCaseworker(userInfo, headers, "IA", "Asylum");
    }

    public void setupCFTOrganisationalRoleAssignment(Headers headers, String roleName) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        createCaseAllocator(userInfo, headers, "IA");
        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            roleName,
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid());
    }

    public void setupCFTOrganisationalRoleAssignment(Headers headers, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createSupervisor(userInfo, headers, jurisdiction);
        createStandardTribunalCaseworker(userInfo, headers, jurisdiction, caseType);

    }

    public void setupCFTOrganisationalRoleAssignmentForChallengedAccess(Headers headers, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUserChallenged(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createSupervisor(userInfo, headers, jurisdiction);
        createStandardTribunalCaseworker(userInfo, headers, jurisdiction, caseType);

    }

    private void createStandardTribunalCaseworker(UserInfo userInfo, Headers headers,
                                                  String jurisdiction, String caseType) {
        log.info("Creating Standard Tribunal caseworker organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(), "tribunal-caseworker",
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "caseType", caseType,
                "jurisdiction", jurisdiction
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupCFTOrganisationalRoleAssignmentForWA(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, "WA");
        createStandardTribunalCaseworker(userInfo, headers, "WA", "WaCaseType");
    }

    public void setupLeadJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createLeaderShipJudge(userInfo, headers, jurisdiction);
        createLeadJudge(userInfo, headers, jurisdiction, caseId);

    }

    public void setupFtpaJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createSeniorJudge(userInfo, headers, jurisdiction);
        createFtpaJudge(userInfo, headers, jurisdiction, caseType, caseId);

    }

    public void setupHearingPanelJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createSeniorJudge(userInfo, headers, jurisdiction);
        createHearingPanelJudge(userInfo, headers, jurisdiction, caseType, caseId);

    }

    public void setupCaseManagerForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");

        createCaseAllocator(userInfo, headers, jurisdiction);
        createStandardTribunalCaseworker(userInfo, headers, jurisdiction, caseType);
        log.info("Creating Case manager Organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "case-manager",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction,
                "substantive", "Y"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupChallengedAccessJudiciary(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUserChallenged(userInfo.getUid(), headers);

        log.info("Creating challenged-access-judiciary Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "challenged-access-judiciary",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction,
                "substantive", "F"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.CHALLENGED.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupChallengedAccessLegalOps(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUserChallenged(userInfo.getUid(), headers);

        log.info("Creating challenged-access-legal-ops Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "challenged-access-legal-ops",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction,
                "substantive", "F"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.CHALLENGED.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupChallengedAccessAdmin(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUserChallenged(userInfo.getUid(), headers);

        log.info("Creating challenged-access-admin Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "challenged-access-admin",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction,
                "substantive", "F"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.CHALLENGED.name(),
            RoleCategory.ADMIN.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupExcludedAccessJudiciary(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        log.info("Creating Conflict of Interest role for judicial users Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "conflict-of-interest",
            toJsonString(Map.of(
                "jurisdiction", jurisdiction,
                "caseType", caseType,
                "caseId", caseId
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.EXCLUDED.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.RESTRICTED.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupCFTOrganisationalWithMultipleRoles(Headers headers) {

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
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(attributes),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(attributes),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
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
            "workTypes", "hearing_work,upper_tribunal,routine_work"
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(attributes),
            R1_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
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
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(attributes),
            "requests/roleAssignment/set-organisational-role-assignment-request-without-end-date.json",
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupCFTJudicialOrganisationalRoleAssignment(Headers headers, String caseId, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        createCaseAllocator(userInfo, headers, jurisdiction);

        log.info("Creating judge");
        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "judge",
            toJsonString(Map.of("jurisdiction", jurisdiction)),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );

        log.info("Creating hearing judge");
        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "hearing-judge",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of("373")),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );

    }

    public void setupCFTAdministrativeOrganisationalRoleAssignment(Headers headers) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        createCaseAllocator(userInfo, headers, "IA");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "hearing-centre-admin",
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.ADMIN.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
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
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(attributes),
            R1_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupRestrictedRoleAssignment(String caseId, Headers headers) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction IA
        log.info("Creating Organizational Role");
        createCaseAllocator(userInfo, headers, "IA");
        createSupervisor(userInfo, headers, "IA");
        createStandardTribunalCaseworker(userInfo, headers, "IA", "Asylum");
        createSpecificTribunalCaseWorker(caseId, headers, userInfo, "IA", "Asylum");
    }

    private void createSpecificTribunalCaseWorker(String caseId, Headers headers, UserInfo userInfo,
                                                  String jurisidction, String caseType) {
        log.info("Creating specific tribunal caseworker organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisidction
            )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createSupervisor(UserInfo userInfo, Headers headers, String jurisdiction) {
        log.info("Creating task supervisor organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "task-supervisor",
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "jurisdiction", jurisdiction
            )),
            "requests/roleAssignment/r2/set-organisational-role-assignment-request.json",
            "STANDARD",
            "LEGAL_OPERATIONS",
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createCaseAllocator(UserInfo userInfo, Headers headers, String jurisdiction) {
        log.info("Creating case allocator organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(), "case-allocator",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.LEGAL_OPERATIONS.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createLeaderShipJudge(UserInfo userInfo, Headers headers, String jurisdiction) {
        log.info("Creating leadership-judge organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "leadership-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createLeadJudge(UserInfo userInfo, Headers headers, String jurisdiction, String caseId) {
        log.info("Creating lead-judge organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "lead-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction,
                    "caseId", caseId
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createSeniorJudge(UserInfo userInfo, Headers headers, String jurisdiction) {
        log.info("Creating senior-judge organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "senior-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.STANDARD.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.ORGANISATION.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createFtpaJudge(UserInfo userInfo, Headers headers, String jurisdiction, String caseType, String caseId) {
        log.info("Creating ftpa-judge organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "ftpa-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction,
                    "caseType", caseType,
                    "caseId", caseId
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    private void createHearingPanelJudge(UserInfo userInfo, Headers headers, String jurisdiction, String caseType, String caseId) {
        log.info("Creating hearing-panel-judge organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "hearing-panel-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction,
                    "caseType", caseType,
                    "caseId", caseId
                )),
            R2_ROLE_ASSIGNMENT_REQUEST,
            GrantType.SPECIFIC.name(),
            RoleCategory.JUDICIAL.name(),
            toJsonString(List.of()),
            RoleType.CASE.name(),
            Classification.PUBLIC.name(),
            "staff-organisational-role-mapping",
            userInfo.getUid(),
            false,
            false,
            null,
            "2020-01-01T00:00:00Z",
            null,
            userInfo.getUid()
        );
    }

    public void setupRestrictedRoleAssignmentForWA(String caseId, Headers headers) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        createCaseAllocator(userInfo, headers, "WA");
        createSupervisor(userInfo, headers, "WA");
        createStandardTribunalCaseworker(userInfo, headers, "WA", "WaCaseType");
        createSpecificTribunalCaseWorker(caseId, headers, userInfo, "WA", "WaCaseType");
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
        /*
        This workaround adjusts for a race condition which usually occurs between xx:00 and xx:15 every hour
        where another task has been created in the database with the same id
         */
        if (result.getStatusCode() != HttpStatus.CREATED.value()) {
            final String errorType = "https://github.com/hmcts/wa-task-management-api/problem/database-conflict";
            result.then().assertThat()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .body("type", equalTo(errorType));
        }

    }

    public void insertTaskInCftTaskDbWithoutWarnings(TestVariables testVariables, String taskType,
                                                     Headers authenticationHeaders) {

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
        /*
        This workaround adjusts for a race condition which usually occurs between xx:00 and xx:15 every hour
        where another task has been created in the database with the same id
         */
        if (result.getStatusCode() != HttpStatus.CREATED.value()) {
            final String errorType = "https://github.com/hmcts/wa-task-management-api/problem/database-conflict";
            result.then().assertThat()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .body("type", equalTo(errorType));
        }
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

    private String toJsonString(List<String> attributes) {
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
                                    String actorId,
                                    String roleName,
                                    String attributes,
                                    String resourceFilename,
                                    String grantType,
                                    String roleCategory,
                                    String authorisations,
                                    String roleType,
                                    String classification,
                                    String process,
                                    String reference,
                                    boolean replaceExisting,
                                    Boolean readOnly,
                                    String notes,
                                    String beginTime,
                                    String endTime,
                                    String assignerId) {

        String body = getBody(caseId, actorId, roleName, resourceFilename, attributes, grantType, roleCategory,
            authorisations, roleType, classification, process, reference, replaceExisting,
            readOnly, notes, beginTime, endTime, assignerId);

        roleAssignmentServiceApi.createRoleAssignment(
            body,
            bearerUserToken,
            s2sToken
        );
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
                .filter(assignment -> RoleType.ORGANISATION.equals(assignment.getRoleType()))
                .collect(toList());

            List<RoleAssignment> caseRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> RoleType.CASE.equals(assignment.getRoleType()))
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

    private void clearAllRoleAssignmentsForUserChallenged(String userId, Headers headers) {
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
                .filter(assignment -> RoleType.ORGANISATION.equals(assignment.getRoleType()))
                .collect(toList());

            List<RoleAssignment> caseRoleAssignments = response.getRoleAssignmentResponse().stream()
                .filter(assignment -> RoleType.CASE.equals(assignment.getRoleType()))
                .collect(toList());

            caseRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );

            organisationalRoleAssignments.forEach(assignment ->
                roleAssignmentServiceApi.deleteRoleAssignmentById(assignment.getId(), userToken, serviceToken)
            );
        }
    }

    private String getBody(final String caseId,
                           String actorId,
                           final String roleName,
                           final String resourceFilename,
                           final String attributes,
                           final String grantType,
                           String roleCategory,
                           String authorisations,
                           String roleType,
                           String classification,
                           String process,
                           String reference,
                           boolean replaceExisting,
                           Boolean readOnly,
                           String notes,
                           String beginTime,
                           String endTime,
                           String assignerId) {

        String assignmentRequestBody = null;

        try {
            assignmentRequestBody = FileUtils.readFileToString(ResourceUtils.getFile(
                "classpath:" + resourceFilename), "UTF-8"
            );
            assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", actorId);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_NAME_PLACEHOLDER}", roleName);
            assignmentRequestBody = assignmentRequestBody.replace("{GRANT_TYPE}", grantType);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_CATEGORY}", roleCategory);
            assignmentRequestBody = assignmentRequestBody.replace("{ROLE_TYPE}", roleType);
            assignmentRequestBody = assignmentRequestBody.replace("{CLASSIFICATION}", classification);
            assignmentRequestBody = assignmentRequestBody.replace("{PROCESS}", process);
            assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", assignerId);

            assignmentRequestBody = assignmentRequestBody.replace(
                "\"replaceExisting\": \"{REPLACE_EXISTING}\"",
                String.format("\"replaceExisting\": %s", replaceExisting)
            );

            if (beginTime != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{BEGIN_TIME_PLACEHOLDER}",
                    beginTime
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"beginTime\": \"{BEGIN_TIME_PLACEHOLDER}\"", "");
            }

            if (endTime != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{END_TIME_PLACEHOLDER}",
                    endTime
                );
            } else {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "{END_TIME_PLACEHOLDER}",
                    ZonedDateTime.now().plusHours(2).format(ROLE_ASSIGNMENT_DATA_TIME_FORMATTER)
                );
            }

            if (attributes != null) {
                assignmentRequestBody = assignmentRequestBody
                    .replace("\"{ATTRIBUTES_PLACEHOLDER}\"", attributes);
            }

            if (caseId != null) {
                assignmentRequestBody = assignmentRequestBody.replace("{CASE_ID_PLACEHOLDER}", caseId);
            }

            assignmentRequestBody = assignmentRequestBody.replace("{REFERENCE}", reference);


            if (notes != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "\"notes\": \"{NOTES}\"",
                    String.format("\"notes\": [%s]", notes)
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"notes\": \"{NOTES}\"", "");
            }

            if (readOnly != null) {
                assignmentRequestBody = assignmentRequestBody.replace(
                    "\"readOnly\": \"{READ_ONLY}\"",
                    String.format("\"readOnly\": %s", readOnly)
                );
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"readOnly\": \"{READ_ONLY}\"", "");
            }

            if (authorisations != null) {
                assignmentRequestBody = assignmentRequestBody.replace("\"{AUTHORISATIONS}\"", authorisations);
            } else {
                assignmentRequestBody = assignmentRequestBody
                    .replace(",\n" + "      \"authorisations\": \"{AUTHORISATIONS}\"", "");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return assignmentRequestBody;
    }

}
