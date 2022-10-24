package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;

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

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
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

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
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
}
