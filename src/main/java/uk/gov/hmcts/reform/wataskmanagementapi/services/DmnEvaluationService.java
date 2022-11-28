package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.jsonValue;

@Slf4j
@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class DmnEvaluationService {

    private final CamundaServiceApi camundaServiceApi;
    private final AuthTokenGenerator serviceAuthTokenGenerator;

    public DmnEvaluationService(CamundaServiceApi camundaServiceApi,
                                AuthTokenGenerator serviceAuthTokenGenerator) {
        this.camundaServiceApi = camundaServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
    }

    public List<PermissionsDmnEvaluationResponse> evaluateTaskPermissionsDmn(String jurisdiction,
                                                                             String caseType,
                                                                             String caseData,
                                                                             String taskAttributes) {
        String decisionTableKey = WA_TASK_PERMISSIONS.getTableKey(jurisdiction, caseType);
        return performEvaluatePermissionsDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData,
            taskAttributes
        );
    }

    public List<ConfigurationDmnEvaluationResponse> evaluateTaskConfigurationDmn(String jurisdiction,
                                                                                 String caseType,
                                                                                 String caseData,
                                                                                 String taskAttributes) {
        String decisionTableKey = WA_TASK_CONFIGURATION.getTableKey(jurisdiction, caseType);
        return performEvaluateConfigurationDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData,
            taskAttributes
        );
    }

    @Cacheable(key = "#jurisdiction", value = "task_types_dmn", sync = true)
    public Set<TaskTypesDmnResponse> getTaskTypesDmn(String jurisdiction, String dmnNameField) {
        Set<TaskTypesDmnResponse> response = performGetTaskTypesDmn(jurisdiction, dmnNameField);
        log.info("task-types-dmn fetched from camunda-api. jurisdiction:{} - taskTypesDmn: {}",
            jurisdiction, response);
        return response;
    }

    @Cacheable(key = "#jurisdiction", value = "task_types", sync = true)
    public List<TaskTypesDmnEvaluationResponse> evaluateTaskTypesDmn(String jurisdiction, String decisionTableKey) {
        List<TaskTypesDmnEvaluationResponse> response =
            performEvaluateTaskTypesDmnAction(
                decisionTableKey,
                jurisdiction
            );
        log.info("task-types fetched from camunda-api. jurisdiction:{} - taskTypesDmnEvaluationResponses: {}",
            jurisdiction, response);
        return response;
    }

    private List<ConfigurationDmnEvaluationResponse> performEvaluateConfigurationDmnAction(
        String decisionTableKey,
        String jurisdiction,
        String caseData,
        String taskAttributes) {
        try {
            return camundaServiceApi.evaluateConfigurationDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(caseData), jsonValue(taskAttributes)))
            );
        } catch (FeignException e) {
            log.error("Case Configuration : Could not evaluate from decision table '{}'", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }

    private List<PermissionsDmnEvaluationResponse> performEvaluatePermissionsDmnAction(
        String decisionTableKey,
        String jurisdiction,
        String caseData,
        String taskAttributes) {
        try {
            return camundaServiceApi.evaluatePermissionsDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(caseData), jsonValue(taskAttributes)))
            );
        } catch (FeignException e) {
            log.error("Case Configuration : Could not evaluate from decision table '{}'", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }

    private Set<TaskTypesDmnResponse> performGetTaskTypesDmn(String jurisdiction, String dmnNameField) {
        try {
            List<TaskTypesDmnResponse> taskTypesDmnResponseList = camundaServiceApi.getTaskTypesDmnTable(
                serviceAuthTokenGenerator.generate(),
                jurisdiction.toLowerCase(Locale.ROOT),
                dmnNameField
            );

            return new HashSet<>(taskTypesDmnResponseList);
        } catch (FeignException e) {
            log.error("Could not get {} from camunda for '{}'", dmnNameField, jurisdiction);
            throw new IllegalStateException(
                String.format("Could not get %s from camunda for %s", dmnNameField, jurisdiction),
                e
            );
        }
    }

    private List<TaskTypesDmnEvaluationResponse> performEvaluateTaskTypesDmnAction(
        String decisionTableKey,
        String jurisdiction) {
        try {
            return camundaServiceApi.evaluateTaskTypesDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>()
            );
        } catch (FeignException e) {
            log.error("Could not evaluate from decision table '{}'", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }
}