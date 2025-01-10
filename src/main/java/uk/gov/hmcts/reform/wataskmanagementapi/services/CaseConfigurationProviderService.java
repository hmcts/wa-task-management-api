package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

@Slf4j
@Component
public class CaseConfigurationProviderService {

    public static final String ADDITIONAL_PROPERTIES_PREFIX = "additionalProperties_";
    public static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";
    private final CcdDataService ccdDataService;
    private final DmnEvaluationService dmnEvaluationService;
    private final ObjectMapper objectMapper;
    private final DateTypeConfigurator dateTypeConfigurator;

    @Value("${config.taskMandatoryFieldsProvidedByClient}")
    List<String> taskMandatoryFieldsProvidedByClient;

    @Autowired
    public CaseConfigurationProviderService(CcdDataService ccdDataService,
                                            DmnEvaluationService dmnEvaluationService,
                                            ObjectMapper objectMapper,
                                            DateTypeConfigurator dateTypeConfigurator) {
        this.ccdDataService = ccdDataService;
        this.dmnEvaluationService = dmnEvaluationService;
        this.objectMapper = objectMapper;
        this.dateTypeConfigurator = dateTypeConfigurator;
    }

    /**
     * Obtains a list of process variables that are related to the ccd case data.
     *
     * @param caseId         the ccd case id
     * @param taskAttributes taskAttributes
     * @return a map with the process variables configuration
     */
    @SuppressWarnings("unchecked")
    public TaskConfigurationResults getCaseRelatedConfiguration(
            String caseId,
            Map<String, Object> taskAttributes,
            boolean isReconfigureRequest) {
        // Obtain case from ccd
        CaseDetails caseDetails = ccdDataService.getCaseData(caseId);

        String caseDataString = writeValueAsString(caseDetails.getData());
        String taskAttributesString = writeValueAsString(taskAttributes);
        log.debug("Case Configuration : task Attributes {}", taskAttributesString);

        validateClientProvidedMandatoryFields(taskAttributes, caseDetails);

        String jurisdiction = caseDetails.getJurisdiction();
        String caseType = caseDetails.getCaseType();

        // Evaluate Dmns
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults =
            dmnEvaluationService.evaluateTaskConfigurationDmn(
                jurisdiction,
                caseType,
                caseDataString,
                taskAttributesString
            );
        log.debug("Case Configuration : taskConfigurationDmn Results {}", taskConfigurationDmnResults);

        taskConfigurationDmnResults
            .forEach(r -> {
                Objects.requireNonNull(r.getName(), "Configuration name cannot be null");
                Objects.requireNonNull(r.getName().getValue(), "Configuration name value cannot be null");
                Objects.requireNonNull(r.getValue(), "Configuration value cannot be null");
            });

        boolean initiationDueDateFound = taskAttributes.containsKey(DUE_DATE.value());

        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResultsAfterUpdate
            = updateTaskConfigurationDmnResultsForAdditionalProperties(
            taskConfigurationDmnResults,
            initiationDueDateFound,
            isReconfigureRequest,
            taskAttributes
        );

        List<PermissionsDmnEvaluationResponse> permissionsDmnResults =
            dmnEvaluationService.evaluateTaskPermissionsDmn(
                jurisdiction,
                caseType,
                caseDataString,
                taskAttributesString
            );
        log.debug("Case Configuration : permissionsDmn Results {}", permissionsDmnResults);
        List<PermissionsDmnEvaluationResponse> filteredPermissionDmnResults
            = permissionsDmnResults.stream()
            .filter(dmnResult -> filterBasedOnCaseAccessCategory(caseDetails, dmnResult))
            .toList();

        Map<String, Object> caseConfigurationVariables = extractDmnResults(
            taskConfigurationDmnResultsAfterUpdate,
            filteredPermissionDmnResults
        );
        log.info("Case Configuration : caseConfiguration Variables {}", caseConfigurationVariables);
        // Enrich case configuration variables with extra variables
        Map<String, Object> allCaseConfigurationValues = new ConcurrentHashMap<>(caseConfigurationVariables);
        allCaseConfigurationValues.put(SECURITY_CLASSIFICATION.value(), caseDetails.getSecurityClassification());
        allCaseConfigurationValues.put(JURISDICTION.value(), caseDetails.getJurisdiction());
        allCaseConfigurationValues.put(CASE_TYPE_ID.value(), caseDetails.getCaseType());

        return new TaskConfigurationResults(
            allCaseConfigurationValues,
            taskConfigurationDmnResultsAfterUpdate,
            filteredPermissionDmnResults
        );
    }

    private void validateClientProvidedMandatoryFields(Map<String, Object> taskAttributes, CaseDetails caseDetails) {
        Map<String, Object> updatedTaskAttributes = new ConcurrentHashMap<>(taskAttributes);
        updatedTaskAttributes.put("caseTypeId", caseDetails.getCaseType());
        updatedTaskAttributes.put("jurisdiction", caseDetails.getJurisdiction());

        List<String> missingFields = new ArrayList<>();
        taskMandatoryFieldsProvidedByClient.forEach(mandatoryField -> {
            Object value = updatedTaskAttributes.get(mandatoryField);
            if (value == null || value.toString().isBlank()) {
                missingFields.add(mandatoryField);
            }
        });

        if (!missingFields.isEmpty()) {
            String missingFieldsMessage = String.join(", ", missingFields);
            log.error("Task Configuration : Mandatory fields not provided by client: {}", missingFieldsMessage);
            throw new BadRequestException(
                String.format("Mandatory fields not provided by client: %s", missingFieldsMessage)
            );
        }
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
        List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults,
        boolean initiationDueDateFound,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes) {

        List<ConfigurationDmnEvaluationResponse> configResponses = taskConfigurationDmnResults;
        if (!isReconfigureRequest) {
            Map<String, Object> additionalProperties = taskConfigurationDmnResults.stream()
                    .filter(r -> r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX))
                    .map(this::removeAdditionalFromCamundaName)
                    //Using optional to allow null values
                    .collect(toMap(r -> r.getName().getValue(), r -> Optional.ofNullable(r.getValue().getValue())));

            configResponses = taskConfigurationDmnResults.stream()
                    .filter(r -> !r.getName().getValue().contains(ADDITIONAL_PROPERTIES_PREFIX))
                    .collect(Collectors.toList());

            if (!additionalProperties.isEmpty()) {
                configResponses.add(new ConfigurationDmnEvaluationResponse(
                        CamundaValue.stringValue(ADDITIONAL_PROPERTIES_KEY),
                        CamundaValue.stringValue(writeValueAsString(additionalProperties))
                ));
            }
        }

        return dateTypeConfigurator.configureDates(
            configResponses,
            initiationDueDateFound,
            isReconfigureRequest,
            taskAttributes
        );
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
            .toList();
        return !commonCategories.isEmpty();
    }

    private Map<String, Object> extractDmnResults(List<ConfigurationDmnEvaluationResponse> taskConfigurationDmnResults,
                                                  List<PermissionsDmnEvaluationResponse> permissionsDmnResults) {

        List<ConfigurationDmnEvaluationResponse> configDmnNullValues = taskConfigurationDmnResults.stream()
            .filter(d -> d.getValue().getValue() == null).toList();

        configDmnNullValues.forEach(d -> log.error(
            "The field '{}' in the configuration DMN file  has a null value ",
            d.getName().getValue()
        ));
        // Combine and Collect all dmns results into a single map
        Map<String, Object> caseConfigurationVariables = new ConcurrentHashMap<>();

        Map<String, Object> configDmnValues = taskConfigurationDmnResults.stream()
            .collect(toMap(
                dmnResult -> dmnResult.getName().getValue(),
                dmnResult -> Optional.ofNullable(dmnResult.getValue().getValue())
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
            objectMapper.registerModule(new Jdk8Module());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Case Configuration : Could not extract case data");
        }
        return null;
    }

}
