package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;

import java.util.List;
import java.util.Locale;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.jsonValue;

@Slf4j
@Component
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
                                                                             String caseData) {
        String decisionTableKey = WA_TASK_PERMISSIONS.getTableKey(jurisdiction, caseType);
        return performEvaluatePermissionsDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData
        );
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public List<ConfigurationDmnEvaluationResponse> evaluateTaskConfigurationDmn(String jurisdiction,
                                                                                 String caseType,
                                                                                 String caseData,
                                                                                 String taskTypeId) {
        String decisionTableKey = WA_TASK_CONFIGURATION.getTableKey(jurisdiction, caseType);
        return performEvaluateConfigurationDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData,
            taskTypeId
        );
    }


    private List<ConfigurationDmnEvaluationResponse> performEvaluateConfigurationDmnAction(String decisionTableKey,
                                                                                           String jurisdiction,
                                                                                           String caseData,
                                                                                           String taskTypeId) {
        try {
            return camundaServiceApi.evaluateConfigurationDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(caseData), CamundaValue.stringValue(taskTypeId)))
            );
        } catch (FeignException e) {
            log.error("Case Configuration : Could not evaluate from decision table '{}'", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }

    private List<PermissionsDmnEvaluationResponse> performEvaluatePermissionsDmnAction(String decisionTableKey,
                                                                                       String jurisdiction,
                                                                                       String caseData) {
        try {
            return camundaServiceApi.evaluatePermissionsDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(
                    new DecisionTableRequest(jsonValue(caseData))
                )
            );
        } catch (FeignException e) {
            log.error("Case Configuration : Could not evaluate from decision table '{}'", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }
}
