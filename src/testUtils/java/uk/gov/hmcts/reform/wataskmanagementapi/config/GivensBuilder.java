package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.documents.Document;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.documents.DocumentNames.NOTICE_OF_APPEAL_PDF;

@Slf4j
public class GivensBuilder {

    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;
    private final DocumentManagementFiles documentManagementFiles;

    private final CoreCaseDataApi coreCaseDataApi;

    public GivensBuilder(RestApiActions camundaApiActions,
                         RestApiActions restApiActions,
                         AuthorizationHeadersProvider authorizationHeadersProvider,
                         CoreCaseDataApi coreCaseDataApi,
                         DocumentManagementFiles documentManagementFiles
    ) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
        this.coreCaseDataApi = coreCaseDataApi;
        this.documentManagementFiles = documentManagementFiles;

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

    public GivensBuilder iCreateATaskWithCaseId(
        String caseId,
        boolean warnings,
        String jurisdiction,
        String caseTypeId) {
        Map<String, CamundaValue<?>> processVariables
            = initiateProcessVariables(caseId, warnings, jurisdiction, caseTypeId);

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

    public GivensBuilder iCreateATaskWithCaseId(String caseId, String taskType) {
        Map<String, CamundaValue<?>> processVariables = initiateProcessVariables(caseId, taskType);

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

    public List<CamundaTask> iRetrieveATasksWithProcessVariableFilter(String key, String value, String taskType) {
        log.info("Attempting to retrieve task with {} = {}", key, value);
        String filter = "?processVariables=" + key + "_eq_" + value + ",taskId_eq_" + taskType;

        AtomicReference<List<CamundaTask>> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = camundaApiActions.get(
                        "/task" + filter,
                        authorizationHeadersProvider.getServiceAuthorizationHeader()
                    );

                    result.then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("size()", is(1));

                    response.set(
                        result.then()
                            .extract()
                            .jsonPath().getList("", CamundaTask.class)
                    );

                    return true;
                });

        return response.get();
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value, int taskCount) {
        String filter = "?processVariables=" + key + "_eq_" + value;

        AtomicReference<List<CamundaTask>> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = camundaApiActions.get(
                        "/task" + filter,
                        authorizationHeadersProvider.getServiceAuthorizationHeader()
                    );

                    result.then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("size()", is(taskCount));

                    response.set(
                        result.then()
                            .extract()
                            .jsonPath().getList("", CamundaTask.class)
                    );

                    return true;
                });

        return response.get();
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

    public GivensBuilder iUpdateTaskVariable(String taskId, Map<String, CamundaValue<?>> processVariables) {
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

    public Map<String, CamundaValue<?>> createDefaultTaskVariables(
        String caseId,
        String jurisdiction,
        String caseTypeId) {
        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("jurisdiction", jurisdiction)
            .withProcessVariable("caseTypeId", caseTypeId)
            .withProcessVariable("region", "1")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "Taylor House")
            .withProcessVariable("staffLocation", "Taylor House")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskId", "reviewTheAppeal")
            .withProcessVariable("taskAttributes", "")
            .withProcessVariable("taskType", "reviewTheAppeal")
            .withProcessVariable("taskCategory", "Case Progression")
            .withProcessVariable("taskState", "unconfigured")
            //for testing-purposes
            .withProcessVariable("dueDate", now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("task-supervisor", "Read,Refer,Manage,Cancel")
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", now().format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("workingDaysAllowed", "2")
            .withProcessVariableBoolean("hasWarnings", false)
            .withProcessVariable("warningList", (new WarningValues()).toString())
            .withProcessVariable("caseManagementCategory", "Protection")
            .withProcessVariable("description", "aDescription")
            .build();

        return processVariables.getProcessVariablesMap();
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariables(String caseId, String taskType) {
        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("jurisdiction", "IA")
            .withProcessVariable("caseTypeId", "Asylum")
            .withProcessVariable("region", "1")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "Taylor House")
            .withProcessVariable("staffLocation", "Taylor House")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskId", taskType)
            .withProcessVariable("taskType", taskType)
            .withProcessVariable("taskCategory", "Case Progression")
            .withProcessVariable("taskState", "unconfigured")
            //for testing-purposes
            .withProcessVariable("dueDate", now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", now().format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("workingDaysAllowed", "2")
            .withProcessVariableBoolean("hasWarnings", false)
            .withProcessVariable("warningList", (new WarningValues()).toString())
            .withProcessVariable("caseManagementCategory", "Protection")
            .withProcessVariable("description", "aDescription")
            .build();

        return processVariables.getProcessVariablesMap();
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariablesWithWarnings(
        String caseId,
        String jurisdiction,
        String caseTypeId
    ) {
        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                        + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";

        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("jurisdiction", jurisdiction)
            .withProcessVariable("caseTypeId", caseTypeId)
            .withProcessVariable("region", "1")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "Taylor House")
            .withProcessVariable("staffLocation", "Taylor House")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskId", "reviewTheAppeal")
            .withProcessVariable("taskType", "reviewTheAppeal")
            .withProcessVariable("taskCategory", "Case Progression")
            .withProcessVariable("taskState", "unconfigured")
            .withProcessVariable("dueDate", now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("task-supervisor", "Read,Refer,Manage,Cancel")
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", now().format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("workingDaysAllowed", "2")
            .withProcessVariableBoolean("hasWarnings", true)
            .withProcessVariable("warningList", values)
            .withProcessVariable("caseManagementCategory", "Protection")
            .withProcessVariable("description", "aDescription")
            .build();

        return processVariables.getProcessVariablesMap();
    }

    public Map<String, CamundaValue<?>> createTaskVariablesForSCSS(String caseId) {
        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "SCSS")
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("region", "1")
            .withProcessVariable("location", "765324")
            .withProcessVariable("locationName", "A Hearing Centre")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("name", "task name")
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("taskState", "unconfigured")
            .withProcessVariable("dueDate", now().plusDays(2).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", now().format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariableBoolean("hasWarnings", false)
            .withProcessVariable("warningList", (new WarningValues()).toString())
            .withProcessVariable("caseManagementCategory", "Protection")
            .withProcessVariable("description", "aDescription")
            .build();

        return processVariables.getProcessVariablesMap();
    }

    public String iCreateACcdCase(String jurisdiction, String caseTypeId, String startEventId, String submitEventId) {
        Headers headers = authorizationHeadersProvider.getLawFirmAuthorization();
        String userToken = headers.getValue(AUTHORIZATION);
        String serviceToken = headers.getValue(SERVICE_AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        Document document = documentManagementFiles.getDocument(NOTICE_OF_APPEAL_PDF);

        StartEventResponse startCase = coreCaseDataApi.startForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseTypeId,
            startEventId
        );

        String resourceFilename = "requests/ccd/case_data.json";

        Map data = null;
        try {
            String caseDataString =
                FileUtils.readFileToString(ResourceUtils.getFile("classpath:" + resourceFilename), "UTF-8");
            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_STORE_URL}",
                document.getDocumentUrl()
            );
            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_NAME}",
                document.getDocumentFilename()
            );
            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_STORE_URL_BINARY}",
                document.getDocumentBinaryUrl()
            );

            data = new ObjectMapper().readValue(caseDataString, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startCase.getToken())
            .event(Event.builder()
                .id(startCase.getEventId())
                .summary("summary")
                .description("description")
                .build())
            .data(data)
            .build();

        //Fire submit event
        CaseDetails caseDetails = coreCaseDataApi.submitForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseTypeId,
            true,
            caseDataContent
        );

        log.info("Created case [" + caseDetails.getId() + "]");

        StartEventResponse submitCase = coreCaseDataApi.startEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseTypeId,
            caseDetails.getId().toString(),
            submitEventId
        );

        CaseDataContent submitCaseDataContent = CaseDataContent.builder()
            .eventToken(submitCase.getToken())
            .event(Event.builder()
                .id(submitCase.getEventId())
                .summary("summary")
                .description("description")
                .build())
            .data(data)
            .build();

        coreCaseDataApi.submitEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseTypeId,
            caseDetails.getId().toString(),
            true,
            submitCaseDataContent
        );
        log.info("Submitted case [" + caseDetails.getId() + "]");

        return caseDetails.getId().toString();
    }

    private RoleAssignmentRequest createRoleAssignmentRequest(String userId, String roleName, String caseId) {
        String process = "case-allocation";
        String reference = caseId + "/" + roleName;
        RoleRequest roleRequest = new RoleRequest(userId, process, reference, true);
        Map<String, String> attributes = Map.of(
            "caseId", caseId
        );
        RoleAssignment roleAssignment = new RoleAssignment(
            ActorIdType.IDAM,
            userId,
            RoleType.CASE,
            roleName,
            Classification.RESTRICTED,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            attributes
        );

        return new RoleAssignmentRequest(
            roleRequest,
            singletonList(roleAssignment)
        );
    }

    private void waitSeconds(int seconds) {
        try {
            log.info("Waiting for {} second(s)", seconds);
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Map<String, CamundaValue<?>> initiateProcessVariables(
        String caseId,
        boolean warnings,
        String jurisdiction,
        String caseTypeId) {
        if (warnings) {
            return createDefaultTaskVariablesWithWarnings(caseId, jurisdiction, caseTypeId);
        } else {
            return createDefaultTaskVariables(caseId, jurisdiction, caseTypeId);
        }
    }

    private Map<String, CamundaValue<?>> initiateProcessVariables(String caseId, String taskType) {
        return createDefaultTaskVariables(caseId, taskType);
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

