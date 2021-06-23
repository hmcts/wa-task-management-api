package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.DecisionTableResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.DmnRequest;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.CamundaValue.jsonValue;

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

    public List<DecisionTableResult> evaluateTaskPermissionsDmn(String jurisdiction,
                                                                String caseType,
                                                                String caseData) {
        String decisionTableKey = WA_TASK_PERMISSIONS.getTableKey(jurisdiction, caseType);
        return performEvaluateDmnAction(decisionTableKey, caseData);
    }


    public List<DecisionTableResult> evaluateTaskConfigurationDmn(String jurisdiction,
                                                                  String caseType,
                                                                  String caseData) {
        String decisionTableKey = WA_TASK_CONFIGURATION.getTableKey(jurisdiction, caseType);
        return performEvaluateDmnAction(decisionTableKey, caseData);
    }

    private List<DecisionTableResult> performEvaluateDmnAction(String decisionTableKey,
                                                               String caseData) {
        try {
            return camundaServiceApi.evaluateDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
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
