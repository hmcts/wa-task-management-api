package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Component
public class CaseConfigurationProviderService {

    private final CcdDataService ccdDataService;
    private final DmnEvaluationService dmnEvaluationService;
    private final ObjectMapper objectMapper;


    @Autowired
    public CaseConfigurationProviderService(CcdDataService ccdDataService,
                                            DmnEvaluationService dmnEvaluationService,
                                            ObjectMapper objectMapper) {
        this.ccdDataService = ccdDataService;
        this.dmnEvaluationService = dmnEvaluationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtains a list of process variables that are related to the ccd case data.
     *
     * @param caseId     the ccd case id
     * @param taskTypeId some task type
     * @return a map with the process variables configuration
     */
    public TaskConfigurationResults getCaseRelatedConfiguration(String caseId, String taskTypeId) {
        // Obtain case from ccd
        CaseDetails caseDetails = ccdDataService.getCaseData(caseId);

        String jurisdiction = caseDetails.getJurisdiction();
        String caseType = caseDetails.getCaseType();

        String caseDataString = extractCaseDataAsString(caseDetails.getData());

        // Evaluate Dmns
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults =
            dmnEvaluationService.evaluateTaskConfigurationDmn(jurisdiction, caseType, caseDataString, taskTypeId);

        List<PermissionsDmnEvaluationResponse> permissionsDmnResults =
            dmnEvaluationService.evaluateTaskPermissionsDmn(jurisdiction, caseType, caseDataString);

        Map<String, Object> caseConfigurationVariables = extractDmnResults(
            taskConfigurationDmnResults,
            permissionsDmnResults
        );

        // Enrich case configuration variables with extra variables
        Map<String, Object> allCaseConfigurationValues = new ConcurrentHashMap<>(caseConfigurationVariables);
        allCaseConfigurationValues.put(SECURITY_CLASSIFICATION.value(), caseDetails.getSecurityClassification());
        allCaseConfigurationValues.put(JURISDICTION.value(), caseDetails.getJurisdiction());
        allCaseConfigurationValues.put(CASE_TYPE_ID.value(), caseDetails.getCaseType());

        return new TaskConfigurationResults(
            allCaseConfigurationValues,
            taskConfigurationDmnResults,
            permissionsDmnResults
        );

    }

    private Map<String, Object> extractDmnResults(List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults,
                                                  List<PermissionsDmnEvaluationResponse> permissionsDmnResults) {

        // Combine and Collect all dmns results into a single map
        Map<String, Object> caseConfigurationVariables = new ConcurrentHashMap<>();

        Map<String, Object> configDmnValues = taskConfigurationDmnResults.stream()
            .collect(toMap(
                dmnResult -> dmnResult.getName().getValue(),
                dmnResult -> dmnResult.getValue().getValue()
            ));

        Map<String, Object> permissionsDmnValues = permissionsDmnResults.stream()
            .collect(toMap(
                dmnResult -> dmnResult.getName().getValue(),
                dmnResult -> dmnResult.getValue().getValue()
            ));

        caseConfigurationVariables.putAll(configDmnValues);
        caseConfigurationVariables.putAll(permissionsDmnValues);

        return caseConfigurationVariables;
    }

    private String extractCaseDataAsString(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Case Configuration : Could not extract case data");
        }
        return null;
    }

}
