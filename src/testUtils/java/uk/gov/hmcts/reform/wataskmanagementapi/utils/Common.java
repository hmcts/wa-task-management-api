package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.http.Headers;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@SuppressWarnings("checkstyle:LineLength")
@Slf4j
public class Common {

    public static final String DEFAULT_TASK_TYPE = "reviewTheAppeal";
    public static final String DEFAULT_TASK_NAME = "A Task";
    public static final WarningValues DEFAULT_WARNINGS = new WarningValues();
    public static final DateTimeFormatter ROLE_ASSIGNMENT_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    public static final String R2_ROLE_ASSIGNMENT_REQUEST = "requests/roleAssignment/r2/set-organisational-role-assignment-request.json";
    public static final String R1_ROLE_ASSIGNMENT_REQUEST = "requests/roleAssignment/set-organisational-role-assignment-request.json";
    public static final String WA_CASE_TYPE = "WaCaseType";
    public static final String WA_JURISDICTION = "WA";
    private final GivensBuilder given;
    private final RestApiActions restApiActions;
    private final RestApiActions camundaApiActions;

    private final RestApiActions workflowApiActions;
    private final AuthorizationProvider authorizationProvider;

    private final IdamService idamService;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Common(GivensBuilder given,
                  RestApiActions restApiActions,
                  RestApiActions camundaApiActions,
                  AuthorizationProvider authorizationProvider,
                  IdamService idamService,
                  RoleAssignmentServiceApi roleAssignmentServiceApi,
                  RestApiActions workflowApiActions) {
        this.given = given;
        this.restApiActions = restApiActions;
        this.camundaApiActions = camundaApiActions;
        this.authorizationProvider = authorizationProvider;
        this.idamService = idamService;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
        this.workflowApiActions = workflowApiActions;
    }

    public void updateTaskWithCustomVariablesOverride(TestVariables task,
                                                      Map<CamundaVariableDefinition, String> variablesToUseAsOverride

    ) {
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            task.getCaseId(),
            WA_JURISDICTION,
            WA_CASE_TYPE,
            DEFAULT_TASK_TYPE,
            DEFAULT_TASK_NAME,
            Map.of()
        );
        variablesToUseAsOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesToUseAsOverride.get(key), "String")));

        given.iUpdateVariablesOfTaskById(task.getTaskId(), processVariables);
    }

    public TestVariables setupWATaskAndRetrieveIds() {
        return setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
    }

    public TestVariables setupWATaskAndRetrieveIds(String taskType, String taskName) {
        return setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", taskType, taskName);
    }

    public TestVariables setupWATaskAndRetrieveIds(String resourceFileName, String taskType, String taskName) {

        String caseId = given.iCreateWACcdCase(resourceFileName);

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, WA_JURISDICTION, WA_CASE_TYPE, taskType, taskName)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), taskType, taskName, DEFAULT_WARNINGS);
    }

    public TestVariables setupWATaskForGivenCaseAndRetrieveIds(String caseId, String taskType, String taskName) {
        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId, "WA", "WaCaseType", taskType, taskName)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), taskType, taskName, DEFAULT_WARNINGS);
    }

    public TestVariables setupWAStandaloneTaskAndRetrieveIds(String resourceFileName, String taskType, String taskName) {

        String caseId = given.iCreateWACcdCase(resourceFileName);

        List<CamundaTask> response = given
            .iSendAMessageToWorkflowApi(caseId, WA_JURISDICTION, WA_CASE_TYPE, taskType, taskName)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), taskType, taskName, DEFAULT_WARNINGS);
    }

    public TestVariables setupWATaskWithWithCustomVariableAndRetrieveIds(CamundaVariableDefinition key, String value, String resourceFileName) {
        String caseId = given.iCreateWACcdCase(resourceFileName);
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(
            caseId,
            "WA",
            "Asylum",
            DEFAULT_TASK_TYPE,
            DEFAULT_TASK_NAME,
            Map.of()
        );
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), DEFAULT_TASK_TYPE, DEFAULT_TASK_NAME, DEFAULT_WARNINGS);
    }

    public TestVariables setupWATaskWithAdditionalPropertiesAndRetrieveIds(Map<String, String> additionalProperties, String resourceFileName, String taskType) {
        String caseId = given.iCreateWACcdCase(resourceFileName);

        Map<String, CamundaValue<?>> processVariables =
            given.createDefaultTaskVariables(caseId, WA_JURISDICTION,
                                             WA_CASE_TYPE, taskType, DEFAULT_TASK_NAME, additionalProperties);

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), taskType, DEFAULT_TASK_NAME, DEFAULT_WARNINGS);

    }

    public TestVariables setupWATaskWithWarningsAndRetrieveIds(String taskType, String taskName) {

        String caseId = given.iCreateWACcdCase("requests/ccd/wa_case_data.json");
        WarningValues warnings = new WarningValues(
            asList(
                new Warning("Code1", "Text1"),
                new Warning("Code2", "Text2")
            ));

        String warningString = "WarningValues(values=[])";
        try {
            warningString = warnings.getValuesAsJson();
        } catch (JsonProcessingException e) {
            fail("Fail to create warning list: " + caseId);
        }

        List<CamundaTask> response = given
            .iCreateATaskWithWarnings(caseId, WA_JURISDICTION, WA_CASE_TYPE, taskType, taskName, warningString)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 1);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used: " + caseId);
        }

        return new TestVariables(caseId, response.get(0).getId(), response.get(0).getProcessInstanceId(), taskType, DEFAULT_TASK_NAME, warnings);
    }

    public void cleanUpTask(String... taskId) {
        Stream.of(taskId).forEach(task -> {
            log.info("Cleaning task {}", task);
            camundaApiActions.post(ENDPOINT_COMPLETE_TASK, task,
                authorizationProvider.getServiceAuthorizationHeadersOnly());
        });
    }

    public void clearAllRoleAssignments(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
    }

    public void setupWAOrganisationalRoleAssignment(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, WA_JURISDICTION);
        createStandardTribunalCaseworker(userInfo, headers, WA_JURISDICTION, WA_CASE_TYPE);
    }

    public void setupWAOrganisationalRoleAssignment(Headers headers, String roleName) {

        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", WA_JURISDICTION
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            roleName,
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
            userInfo.getUid());
    }

    public void setupWAOrganisationalRoleAssignmentWithCustomAttributes(Headers headers, Map<String, String> attributes) {

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

    public void setupWAOrganisationalRoleAssignmentWithWorkTypes(Headers headers, String roleName) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        Map<String, String> attributes = Map.of(
            "primaryLocation", "765324",
            "region", "1",
            //This value must match the camunda task location variable for the permission check to pass
            "baseLocation", "765324",
            "jurisdiction", WA_JURISDICTION,
            "workTypes", "hearing_work,upper_tribunal,routine_work"
        );

        //Clean/Reset user
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        //Creates an organizational role for jurisdiction WA
        log.info("Creating Organizational Role");
        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            roleName,
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

    public void setupCFTOrganisationalRoleAssignment(Headers headers, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, jurisdiction);
        createSupervisor(userInfo, headers, jurisdiction);
        createStandardTribunalCaseworker(userInfo, headers, jurisdiction, caseType);
    }

    public void setupCFTCtscRoleAssignmentForWA(Headers headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createCaseAllocator(userInfo, headers, WA_JURISDICTION);
        createCtscCaseworker(userInfo, headers, WA_JURISDICTION, WA_CASE_TYPE);
    }

    public void setupHearingPanelJudgeForStandardAccess(Headers headers, String jurisdiction, String caseType) {
        log.info("Creating hearing-panel-judge organizational Role");
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "hearing-panel-judge",
            toJsonString(
                Map.of(
                    "primaryLocation", "765324",
                    "jurisdiction", jurisdiction,
                    "caseType", caseType
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

    public void setupStandardCaseManager(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        log.info("Creating Case manager Organizational Role");

        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "case-manager",
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

    public void setupCaseManagerForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        log.info("Creating Case manager Case Role");

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

    public void setupExcludedAccessJudiciary(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        log.info("Creating Conflict of Interest case role for judicial users Role");

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

    public void setupLeadJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction) {
        log.info("Creating lead-judge Case Role");

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

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

    public void setupFtpaJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {
        setupFtpaJudgeForSpecificAccess(headers, caseId, jurisdiction, caseType, true);
    }

    public void setupFtpaJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType, boolean clearRoleAssignments) {
        log.info("Creating ftpa-judge Case Role");

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        if (clearRoleAssignments) {
            clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        }

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

    public void setupFtpaJudgeForCaseAccess(Headers headers, String caseId, String jurisdiction, String caseType) {

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);
        createStandardTribunalCaseworker(userInfo, headers, jurisdiction, caseType);
        setupFtpaJudgeForSpecificAccess(headers, caseId, jurisdiction, caseType, false);
        //createFtpaJudge(userInfo, headers, jurisdiction, caseType, caseId);
    }

    public void setupHearingPanelJudgeForSpecificAccess(Headers headers, String caseId, String jurisdiction, String caseType) {
        log.info("Creating hearing-panel-judge Case Role");

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

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

    public void setupSpecificTribunalCaseWorker(String caseId, Headers headers, String jurisdiction, String caseType) {
        log.info("Creating specific tribunal caseworker organizational Role");

        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);


        postRoleAssignment(
            caseId,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "tribunal-caseworker",
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction
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

    public void setupChallengedAccessLegalOps(Headers headers, String caseId, String jurisdiction, String caseType) {
        UserInfo userInfo = authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION));

        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

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

        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

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

    private void createCtscCaseworker(UserInfo userInfo, Headers headers,
                                      String jurisdiction, String caseType) {
        log.info("Creating CTSC caseworker organizational Role");

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            "ctsc",
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "caseType", caseType,
                "jurisdiction", jurisdiction
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

    public void createStandardWARoleAssignment(Headers headers, String roleName) {
        log.info("Creating Standard {} organizational Role", roleName);
        UserInfo userInfo = idamService.getUserInfo(headers.getValue(AUTHORIZATION));
        clearAllRoleAssignmentsForUser(userInfo.getUid(), headers);

        postRoleAssignment(
            null,
            headers.getValue(AUTHORIZATION),
            headers.getValue(SERVICE_AUTHORIZATION),
            userInfo.getUid(),
            roleName,
            toJsonString(Map.of(
                "primaryLocation", "765324",
                "caseType", "WaCaseType",
                "jurisdiction", "WA"
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
                UserInfo userInfo = authorizationProvider.getUserInfo(userToken);
                createCaseAllocator(userInfo, headers, WA_JURISDICTION);
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
