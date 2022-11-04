package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.ZonedDateTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

@Slf4j
public class GivensBuilder {

    private final RestApiActions camundaApiActions;
    private final RestApiActions workflowApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationProvider authorizationProvider;
    private final DocumentManagementFiles documentManagementFiles;

    private final CcdRetryClient ccdRetryClient;


    private Map<String, CamundaValue<?>> taskVariables;

    public GivensBuilder(RestApiActions camundaApiActions,
                         RestApiActions restApiActions,
                         AuthorizationProvider authorizationProvider,
                         CcdRetryClient ccdRetryClient,
                         DocumentManagementFiles documentManagementFiles,
                         RestApiActions workflowApiActions
    ) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
        this.ccdRetryClient = ccdRetryClient;
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

    public String iCreateACcdCase() {
        TestAuthenticationCredentials lawFirmCredentials =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-r2-");
        return ccdRetryClient.createCCDCaseWithJurisdictionAndCaseTypeAndEvent(
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
        return ccdRetryClient.createCCDCaseWithJurisdictionAndCaseTypeAndEvent(
            "WA",
            "WaCaseType",
            "CREATE",
            "START_PROGRESS",
            lawFirmCredentials,
            resourceFileName
        );
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

