package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Component
public class CaseConfigurationProviderService {

    public static final String ADDITIONAL_PROPERTIES_PREFIX = "additionalProperties_";
    public static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";
    private final CcdDataService ccdDataService;
    private final DmnEvaluationService dmnEvaluationService;
    private final ObjectMapper objectMapper;
    private final DueDateCalculator dueDateCalculator;

    @Autowired
    public CaseConfigurationProviderService(CcdDataService ccdDataService,
                                            DmnEvaluationService dmnEvaluationService,
                                            ObjectMapper objectMapper,
                                            DueDateCalculator dueDateCalculator) {
        this.ccdDataService = ccdDataService;
        this.dmnEvaluationService = dmnEvaluationService;
        this.objectMapper = objectMapper;
        this.dueDateCalculator = dueDateCalculator;
    }

    /**
     * Obtains a list of process variables that are related to the ccd case data.
     *
     * @param caseId         the ccd case id
     * @param taskAttributes taskAttributes
     * @return a map with the process variables configuration
     */
    public TaskConfigurationResults getCaseRelatedConfiguration(String caseId, Map<String, Object> taskAttributes) {
        // Obtain case from ccd
        CaseDetails caseDetails = ccdDataService.getCaseData(caseId);

        String jurisdiction = caseDetails.getJurisdiction();
        String caseType = caseDetails.getCaseType();

        String caseDataString = writeValueAsString(caseDetails.getData());
        String taskAttributesString = writeValueAsString(taskAttributes);

        // Evaluate Dmns
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults =
            dmnEvaluationService.evaluateTaskConfigurationDmn(
                jurisdiction,
                caseType,
                caseDataString,
                taskAttributesString
            );

        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResultsWithAdditionalProperties
            = updateTaskConfigurationDmnResultsForAdditionalProperties(taskConfigurationDmnResults, jurisdiction);

        List<PermissionsDmnEvaluationResponse> permissionsDmnResults =
            dmnEvaluationService.evaluateTaskPermissionsDmn(
                jurisdiction,
                caseType,
                caseDataString,
                taskAttributesString
            );

        List<PermissionsDmnEvaluationResponse> filteredPermissionDmnResults
            = permissionsDmnResults.stream()
            .filter(dmnResult -> filterBasedOnCaseAccessCategory(caseDetails, dmnResult))
            .collect(Collectors.toList());

        Map<String, Object> caseConfigurationVariables = extractDmnResults(
            taskConfigurationDmnResultsWithAdditionalProperties,
            filteredPermissionDmnResults
        );

        // Enrich case configuration variables with extra variables
        Map<String, Object> allCaseConfigurationValues = new ConcurrentHashMap<>(caseConfigurationVariables);
        allCaseConfigurationValues.put(SECURITY_CLASSIFICATION.value(), caseDetails.getSecurityClassification());
        allCaseConfigurationValues.put(JURISDICTION.value(), caseDetails.getJurisdiction());
        allCaseConfigurationValues.put(CASE_TYPE_ID.value(), caseDetails.getCaseType());

        return new TaskConfigurationResults(
            allCaseConfigurationValues,
            taskConfigurationDmnResultsWithAdditionalProperties,
            filteredPermissionDmnResults
        );
    }

    public List<ConfigurationDmnEvaluationResponse> evaluateConfigurationDmn(
        String caseId,
        Map<String, Object> taskAttributes) {
        // Obtain case from ccd
        CaseDetails caseDetails = ccdDataService.getCaseData(caseId);

        String jurisdiction = caseDetails.getJurisdiction();
        String caseType = caseDetails.getCaseType();

        String caseDataString = writeValueAsString(caseDetails.getData());
        String taskAttributesString = writeValueAsString(taskAttributes);

        // Evaluate Dmns
        return
            dmnEvaluationService.evaluateTaskConfigurationDmn(
                jurisdiction,
                caseType,
                caseDataString,
                taskAttributesString
            );
    }

    private List<ConfigurationDmnEvaluationResponse> updateTaskConfigurationDmnResultsForAdditionalProperties(
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults, String jurisdiction) {

        Map<String, Object> additionalProperties = taskConfigurationDmnResults.stream()
            .filter(r -> r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX))
            .map(this::removeAdditionalFromCamundaName)
            .collect(toMap(r -> r.getName().getValue(), r -> r.getValue().getValue()));

        List<ConfigurationDmnEvaluationResponse> configResponses = taskConfigurationDmnResults.stream()
            .filter(r -> !r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX)).collect(Collectors.toList());

        if (!additionalProperties.isEmpty()) {
            configResponses.add(new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue(ADDITIONAL_PROPERTIES_KEY),
                CamundaValue.stringValue(writeValueAsString(additionalProperties))
            ));
        }

        return dueDateCalculator.calculateDueDate(configResponses, jurisdiction);
    }

    private ConfigurationDmnEvaluationResponse removeAdditionalFromCamundaName(
        ConfigurationDmnEvaluationResponse resp) {
        String additionalPropKey = resp.getName().getValue().replace(ADDITIONAL_PROPERTIES_PREFIX, "");
        return new ConfigurationDmnEvaluationResponse(CamundaValue.stringValue(additionalPropKey), resp.getValue());
    }

    private boolean filterBasedOnCaseAccessCategory(CaseDetails caseDetails,
                                                    PermissionsDmnEvaluationResponse dmnResult) {
        CamundaValue<String> caseAccessCategory = dmnResult.getCaseAccessCategory();
        if (caseAccessCategory == null || caseAccessCategory.getValue() == null
            || caseAccessCategory.getValue().isBlank()) {
            return true;
        }

        Object caseAccessCategoryFromCase = caseDetails.getData().get("caseAccessCategory");
        if (caseAccessCategoryFromCase == null || ((String) caseAccessCategoryFromCase).isBlank()) {
            return false;
        }

        List<String> caseAccessCategories = Arrays.asList(caseAccessCategory.getValue().split(","));
        List<String> caseFromCategoriesFromCase = Arrays.asList(((String) caseAccessCategoryFromCase).split(","));

        List<String> commonCategories = caseAccessCategories.stream()
            .filter(caseFromCategoriesFromCase::contains)
            .collect(Collectors.toList());
        return !commonCategories.isEmpty();
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

    private String writeValueAsString(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Case Configuration : Could not extract case data");
        }
        return null;
    }

}
