package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.documents.Document;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.ZonedDateTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.documents.DocumentNames.NOTICE_OF_APPEAL_PDF;

@Slf4j
public class GivensBuilder {

    private final RestApiActions camundaApiActions;
    private final RestApiActions workflowApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationProvider authorizationProvider;
    private final DocumentManagementFiles documentManagementFiles;

    private final AtomicInteger nextHearingDateCounter = new AtomicInteger();
    private final CcdRetryableClient ccdRetryableClient;


    public GivensBuilder(RestApiActions camundaApiActions,
                         RestApiActions restApiActions,
                         AuthorizationProvider authorizationProvider,
                         CcdRetryableClient ccdRetryableClient,
                         DocumentManagementFiles documentManagementFiles,
                         RestApiActions workflowApiActions
    ) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
        this.ccdRetryableClient = ccdRetryableClient;
        this.documentManagementFiles = documentManagementFiles;
        this.workflowApiActions = workflowApiActions;

    }

    public GivensBuilder iCreateATaskWithCustomVariables(Map<String, CamundaValue<?>> processVariables) {
        createTask(processVariables);
        return this;
    }

    public GivensBuilder iCreateATaskWithCaseId(String caseId, String jurisdiction, String caseType,
                                                String taskType, String taskName) {
        Map<String, CamundaValue<?>> processVariables = createDefaultTaskVariables(caseId, jurisdiction, caseType,
            taskType, taskName, Map.of()
        );
        createTask(processVariables);
        return this;
    }

    public GivensBuilder iSendAMessageToWorkflowApi(String caseId, String jurisdiction, String caseType,
                                                    String taskType, String taskName) {
        Map<String, DmnValue<?>> processVariables = standaloneProcessVariables(caseId, jurisdiction, caseType,
            taskType, taskName, now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER));
        postMessageToWorkflowApi(processVariables);
        return this;
    }

    public GivensBuilder iCreateATaskWithWarnings(String caseId, String jurisdiction, String caseType, String taskType,
                                                  String taskName, String warnings) {
        Map<String, CamundaValue<?>> processVariables
            = createDefaultTaskVariablesWithWarnings(
            caseId,
            jurisdiction,
            caseType,
            taskType,
            taskName,
            warnings,
            Map.of()
        );
        createTask(processVariables);

        return this;
    }

    private void createTask(Map<String, CamundaValue<?>> processVariables) {
        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables
        );

        Response result = camundaApiActions.post(
            "message",
            request,
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private void postMessageToWorkflowApi(Map<String, DmnValue<?>> processVariables) {
        SendMessageRequest request = new SendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables,
            null,
            false
        );

        Response result = workflowApiActions.post(
            "/workflow/message",
            request,
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value, int taskIndex) {
        String filter = "?processVariables=" + key + "_eq_" + value;
        return retrieveTasks(filter, taskIndex);
    }

    private List<CamundaTask> retrieveTasks(String filter, int taskIndex) {
        AtomicReference<List<CamundaTask>> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1, SECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = camundaApiActions.get(
                        "/task" + filter,
                        authorizationProvider.getServiceAuthorizationHeader()
                    );

                    result.then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("size()", is(taskIndex));

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

    public void iClaimATaskWithIdAndAuthorization(String taskId, Headers headers, HttpStatus status) {
        Response result = restApiActions.post(
            "task/{task-id}/claim",
            taskId,
            headers
        );

        result.then().assertThat()
            .statusCode(status.value());
    }

    public GivensBuilder iUpdateVariablesOfTaskById(String taskId, Map<String, CamundaValue<?>> processVariables) {
        Response result = camundaApiActions.post(
            "/task/{task-id}/variables",
            taskId,
            new Modifications(processVariables),
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return this;
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariables(
        String caseId,
        String jurisdiction,
        String caseTypeId,
        String taskType,
        String taskName,
        Map<String, String> additionalProperties) {

        return createTaskVariables(caseId,
            jurisdiction,
            caseTypeId,
            "1",
            "765324",
            "Taylor House",
            "Taylor House",
            "PUBLIC",
            taskName,
            taskType,
            taskType,
            "Case Progression",
            "unconfigured",
            now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER),
            now().format(CAMUNDA_DATA_TIME_FORMATTER),
            "2",
            false,
            (new WarningValues()).toString(),
            "Protection",
            "aDescription",
            additionalProperties
        );
    }

    public Map<String, CamundaValue<?>> createDefaultTaskVariablesWithWarnings(
        String caseId,
        String jurisdiction,
        String caseTypeId,
        String taskType,
        String taskName,
        String warnings,
        Map<String, String> additionalProperties
    ) {
        return createTaskVariables(caseId,
            jurisdiction,
            caseTypeId,
            "1",
            "765324",
            "Taylor House",
            "Taylor House",
            "PUBLIC",
            taskName,
            taskType,
            taskType,
            "Case Progression",
            "unconfigured",
            now().plusDays(10).format(CAMUNDA_DATA_TIME_FORMATTER),
            now().format(CAMUNDA_DATA_TIME_FORMATTER),
            "2",
            true,
            warnings,
            "Protection",
            "aDescription",
            additionalProperties
        );
    }

    private Map<String, CamundaValue<?>> createTaskVariables(
        String caseId,
        String jurisdiction,
        String caseTypeId,
        String region,
        String location,
        String locationName,
        String staffLocation,
        String securityClassification,
        String taskName,
        String taskId,
        String taskType,
        String taskCategory,
        String taskState,
        String dueDate,
        String delayUntil,
        String workingDaysAllowed,
        Boolean hasWarnings,
        String warningList,
        String caseManagementCategory,
        String description,
        Map<String, String> additionalProperties) {
        var processVariables = processVariables()
            .withProcessVariable("caseId", caseId)
            .withProcessVariable("jurisdiction", jurisdiction)
            .withProcessVariable("caseTypeId", caseTypeId)
            .withProcessVariable("region", region)
            .withProcessVariable("location", location)
            .withProcessVariable("locationName", locationName)
            .withProcessVariable("staffLocation", staffLocation)
            .withProcessVariable("securityClassification", securityClassification)
            .withProcessVariable("name", taskName)
            .withProcessVariable("taskId", taskId)
            .withProcessVariable("taskType", taskType)
            .withProcessVariable("taskCategory", taskCategory)
            .withProcessVariable("taskState", taskState)
            .withProcessVariable("dueDate", dueDate)
            .withProcessVariable("task-supervisor", "Read,Refer,Manage,Cancel")
            .withProcessVariable("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel")
            .withProcessVariable("delayUntil", delayUntil)
            .withProcessVariable("workingDaysAllowed", workingDaysAllowed)
            .withProcessVariableBoolean("hasWarnings", hasWarnings)
            .withProcessVariable("warningList", warningList)
            .withProcessVariable("caseManagementCategory", caseManagementCategory)
            .withProcessVariable("description", description);

        additionalProperties.forEach(processVariables::withProcessVariable);

        return processVariables.build().getProcessVariablesMap();
    }

    public String iCreateWACcdCase(String resourceFileName) {
        TestAuthenticationCredentials lawFirmCredentials =
            authorizationProvider.getNewWaTribunalCaseworker("wa-ft-r2-");
        return createCCDCaseWithJurisdictionAndCaseTypeAndEvent(
            "WA",
            "WaCaseType",
            "CREATE",
            "START_PROGRESS",
            lawFirmCredentials,
            resourceFileName
        );
    }

    public String createCCDCaseWithJurisdictionAndCaseTypeAndEvent(String jurisdiction,
                                                                    String caseType,
                                                                    String startEventId,
                                                                    String submitEventId,
                                                                    TestAuthenticationCredentials credentials,
                                                                    String resourceFilename) {

        String userToken = credentials.getHeaders().getValue(AUTHORIZATION);
        String serviceToken = credentials.getHeaders().getValue(SERVICE_AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(userToken);

        Document document = documentManagementFiles.getDocumentAs(NOTICE_OF_APPEAL_PDF, credentials);

        StartEventResponse startCase = ccdRetryableClient.startForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            startEventId
        );

        Map data = null;
        try {
            String caseDataString = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:" + resourceFilename),
                "UTF-8"
            );

            caseDataString = caseDataString.replace(
                "{NEXT_HEARING_DATE}",
                OffsetDateTime.now().plusMinutes(nextHearingDateCounter.incrementAndGet()).toString()
            );

            // This code is mad next hearing date sortable
            if (nextHearingDateCounter.get() > 10) {
                nextHearingDateCounter.set(0);
            }

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
        CaseDetails caseDetails = ccdRetryableClient.submitForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            true,
            caseDataContent
        );

        log.info("Created case [" + caseDetails.getId() + "]");

        StartEventResponse submitCase = ccdRetryableClient.startEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
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

        ccdRetryableClient.submitEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            caseDetails.getId().toString(),
            true,
            submitCaseDataContent
        );
        log.info("Submitted case [" + caseDetails.getId() + "]");

        authorizationProvider.deleteAccount(credentials.getAccount().getUsername());

        return caseDetails.getId().toString();
    }

    public Map<String, DmnValue<?>> standaloneProcessVariables(String caseId,
                                                               String jurisdiction,
                                                               String caseType,
                                                               String taskType,
                                                               String taskName,
                                                               String dueDate
    ) {
        Map<String, DmnValue<?>> processVariables = new HashMap<>();
        processVariables.put("dueDate", DmnValue.dmnStringValue(dueDate));
        processVariables.put("name", DmnValue.dmnStringValue(taskName));
        processVariables.put("jurisdiction", DmnValue.dmnStringValue(jurisdiction));
        processVariables.put("caseType", DmnValue.dmnStringValue(caseType));
        processVariables.put("taskType", DmnValue.dmnStringValue(taskType));
        processVariables.put("caseId", DmnValue.dmnStringValue(caseId));
        processVariables.put("idempotencyKey", DmnValue.dmnStringValue(UUID.randomUUID().toString()));

        return processVariables;
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

