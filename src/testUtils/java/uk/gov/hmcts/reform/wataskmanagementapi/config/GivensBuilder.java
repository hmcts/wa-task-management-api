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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.documents.Document;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.ZonedDateTime.now;
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
    private final AuthorizationProvider authorizationProvider;
    private final DocumentManagementFiles documentManagementFiles;

    private final CoreCaseDataApi coreCaseDataApi;
    private Map<String, CamundaValue<?>> taskVariables;

    public GivensBuilder(RestApiActions camundaApiActions,
                         RestApiActions restApiActions,
                         AuthorizationProvider authorizationProvider,
                         CoreCaseDataApi coreCaseDataApi,
                         DocumentManagementFiles documentManagementFiles
    ) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
        this.coreCaseDataApi = coreCaseDataApi;
        this.documentManagementFiles = documentManagementFiles;

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

    public GivensBuilder iCreateATaskWithWarnings(String caseId, String jurisdiction, String caseType, String taskType,
                                                  String taskName, String warnings) {
        Map<String, CamundaValue<?>> processVariables
            = createDefaultTaskVariablesWithWarnings(caseId, jurisdiction, caseType, taskType, taskName, warnings);
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

    public List<CamundaTask> iRetrieveATasksWithProcessVariableFilter(String key, String value, String taskType) {
        log.info("Attempting to retrieve task with {} = {}", key, value);
        String filter = "?processVariables=" + key + "_eq_" + value + ",taskId_eq_" + taskType;
        return retrieveTasks(filter, 1);
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

    public GivensBuilder iUpdateTaskVariable(String taskId, Map<String, CamundaValue<?>> processVariables) {
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
        String warnings
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
                                   Map.of()
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

    public String iCreateACcdCase() {
        TestAuthenticationCredentials lawFirmCredentials =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-r2-");
        return createCCDCaseWithJurisdictionAndCaseTypeAndEvent(
            "IA",
            "Asylum",
            "startAppeal",
            "submitAppeal",
            lawFirmCredentials,
            "requests/ccd/case_data.json"
        );
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

    private String createCCDCaseWithJurisdictionAndCaseTypeAndEvent(String jurisdiction,
                                                                    String caseType,
                                                                    String startEventId,
                                                                    String submitEventId,
                                                                    TestAuthenticationCredentials credentials,
                                                                    String resourceFilename) {

        String userToken = credentials.getHeaders().getValue(AUTHORIZATION);
        String serviceToken = credentials.getHeaders().getValue(SERVICE_AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(userToken);

        Document document = documentManagementFiles.getDocumentAs(NOTICE_OF_APPEAL_PDF, credentials);

        StartEventResponse startCase = coreCaseDataApi.startForCaseworker(
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
                OffsetDateTime.now().toString()
            );

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
            caseType,
            true,
            caseDataContent
        );

        log.info("Created case [" + caseDetails.getId() + "]");

        StartEventResponse submitCase = coreCaseDataApi.startEventForCaseWorker(
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

        coreCaseDataApi.submitEventForCaseWorker(
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

