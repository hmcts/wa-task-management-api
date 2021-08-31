package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.EvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;

import java.util.List;

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

    @SuppressWarnings("unchecked")
    public List<PermissionsDmnEvaluationResponse> evaluateTaskPermissionsDmn(String jurisdiction,
                                                                             String caseType,
                                                                             String caseData) {
        String decisionTableKey = WA_TASK_PERMISSIONS.getTableKey(jurisdiction, caseType);
        return (List<PermissionsDmnEvaluationResponse>) performEvaluateDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData
        );
    }

    @SuppressWarnings("unchecked")
    public List<ConfigurationDmnEvaluationResponse> evaluateTaskConfigurationDmn(String jurisdiction,
                                                                                 String caseType,
                                                                                 String caseData) {
        String decisionTableKey = WA_TASK_CONFIGURATION.getTableKey(jurisdiction, caseType);
        return (List<ConfigurationDmnEvaluationResponse>) performEvaluateDmnAction(
            decisionTableKey,
            jurisdiction,
            caseData
        );
    }

    private List<? extends EvaluationResponse> performEvaluateDmnAction(String decisionTableKey,
                                                                        String jurisdiction,
                                                                        String caseData) {
        try {
            return camundaServiceApi.evaluateDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction,
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
