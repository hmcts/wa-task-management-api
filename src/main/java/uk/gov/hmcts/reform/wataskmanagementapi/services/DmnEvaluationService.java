package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.jsonValue;

@Slf4j
@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class DmnEvaluationService {

    private final CamundaServiceApi camundaServiceApi;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final CamundaObjectMapper camundaObjectMapper;

    public DmnEvaluationService(CamundaServiceApi camundaServiceApi,
                                AuthTokenGenerator serviceAuthTokenGenerator,
                                CamundaObjectMapper camundaObjectMapper) {
        this.camundaServiceApi = camundaServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.camundaObjectMapper = camundaObjectMapper;
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

    @Cacheable(key = "#jurisdiction", value = "task_types_dmn", sync = true, cacheManager = "taskTypeCacheManager")
    public Set<TaskTypesDmnResponse> retrieveTaskTypesDmn(String jurisdiction, String dmnNameField) {
        Set<TaskTypesDmnResponse> response = performRetrieveTaskTypesDmn(jurisdiction, dmnNameField);
        log.info("task-types-dmn fetched from camunda-api. jurisdiction:{} - taskTypesDmn: {}",
            jurisdiction, response);
        return response;
    }

    @Cacheable(key = "#jurisdiction", value = "task_types", sync = true, cacheManager = "taskTypeCacheManager")
    public List<TaskTypesDmnEvaluationResponse> evaluateTaskTypesDmn(String jurisdiction, String decisionTableKey) {
        List<TaskTypesDmnEvaluationResponse> response =
            performEvaluateTaskTypesDmnAction(decisionTableKey, jurisdiction);
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
            List<ConfigurationDmnEvaluationResponse> dmnResponse = camundaServiceApi.evaluateConfigurationDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(caseData), jsonValue(taskAttributes)))
            );
            return dmnResponse.stream().map(CamundaHelper::removeSpaces).collect(Collectors.toList());
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
            List<PermissionsDmnEvaluationResponse> dmnResponse = camundaServiceApi.evaluatePermissionsDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>(new DecisionTableRequest(jsonValue(caseData), jsonValue(taskAttributes)))
            );
            return dmnResponse.stream().map(CamundaHelper::removeSpaces).collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Case Configuration : Could not evaluate from decision table {}", decisionTableKey);
            throw new IllegalStateException(
                String.format("Could not evaluate from decision table %s", decisionTableKey),
                e
            );
        }
    }

    private Set<TaskTypesDmnResponse> performRetrieveTaskTypesDmn(String jurisdiction, String dmnNameField) {
        try {
            List<TaskTypesDmnResponse> taskTypesDmnResponseList = camundaServiceApi.getTaskTypesDmnTable(
                serviceAuthTokenGenerator.generate(),
                jurisdiction.toLowerCase(Locale.ROOT),
                dmnNameField
            );

            return new HashSet<>(taskTypesDmnResponseList);
        } catch (FeignException.ServiceUnavailable | FeignException.GatewayTimeout ex) {
            log.error("An error occurred when getting task-type dmn due to service unavailable. "
                    + "Could not get {} from camunda for {}. Exception: {}",
                dmnNameField, jurisdiction, ex.getMessage());

            throw ex;
        } catch (FeignException ex) {
            log.error("An error occurred when getting task-type dmn. "
                    + "Could not get {} from camunda for {}. Exception: {}",
                dmnNameField, jurisdiction, ex.getMessage());

            Optional<CamundaExceptionMessage> camundaException = readCamundaException(ex);

            camundaException.ifPresent(camundaExceptionMessage ->
                log.error("An error occurred when getting task-type dmn. CamundaException type:{} message:{}",
                    camundaExceptionMessage.getType(), camundaExceptionMessage.getMessage()));

            throw ex;
        }
    }

    private List<TaskTypesDmnEvaluationResponse> performEvaluateTaskTypesDmnAction(
        String decisionTableKey, String jurisdiction) {

        try {
            return camundaServiceApi.evaluateTaskTypesDmnTable(
                serviceAuthTokenGenerator.generate(),
                decisionTableKey,
                jurisdiction.toLowerCase(Locale.ROOT),
                new DmnRequest<>()
            );
        } catch (FeignException.ServiceUnavailable | FeignException.GatewayTimeout ex) {
            log.error("An error occurred when evaluating task-type dmn due to service unavailable. "
                    + "jurisdiction:{} - decisionTableKey:{}. Exception:{}",
                jurisdiction, decisionTableKey, ex.getMessage());

            throw ex;
        } catch (FeignException ex) {
            log.error("An error occurred when evaluating task-type dmn. "
                    + "jurisdiction:{} - decisionTableKey:{}. Exception:{}",
                jurisdiction, decisionTableKey, ex.getMessage());

            Optional<CamundaExceptionMessage> camundaException = readCamundaException(ex);

            camundaException.ifPresent(camundaExceptionMessage ->
                log.error("An error occurred when evaluating task-type dmn. CamundaException type:{} message:{}",
                    camundaExceptionMessage.getType(), camundaExceptionMessage.getMessage()));

            throw ex;
        }
    }

    private Optional<CamundaExceptionMessage> readCamundaException(FeignException ex) {
        try {
            return Optional.of(camundaObjectMapper.readValue(
                ex.contentUTF8(), CamundaExceptionMessage.class));
        } catch (Exception e) {
            log.error("An error occurred when reading CamundaException. Exception:{}", ex.getMessage());
            return Optional.empty();
        }
    }

}
