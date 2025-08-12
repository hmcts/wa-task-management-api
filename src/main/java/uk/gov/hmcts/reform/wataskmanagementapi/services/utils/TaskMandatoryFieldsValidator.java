package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.launchdarkly.sdk.LDValue;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ROLE_CATEGORY;

/**
 * Service to validate mandatory fields of a task.
 */
@Slf4j
@Service
@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public class TaskMandatoryFieldsValidator {

    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final boolean taskMandatoryFieldCheckEnabled;
    private final List<String> taskMandatoryFields;
    public static final LDValue MANDATORY_FIELD_CHECK_FLAG_VARIANT = LDValue.of("jurisdictions");
    private final JsonParserUtils jsonParserUtils;

    private final List<String> tmSpecificMandatoryFields = List.of("taskId", "state","executionTypeCode","created",
                                                                   "dueDateTime", "majorPriority", "minorPriority");

    /**
     * Constructor for TaskMandatoryFieldsValidator.
     *
     * @param launchDarklyFeatureFlagProvider the LaunchDarkly feature flag provider
     * @param taskMandatoryFieldCheckEnabled  flag to enable or disable mandatory field check
     * @param taskMandatoryFields             list of mandatory fields to be checked
     */
    @Autowired
    public TaskMandatoryFieldsValidator(LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                        @Value("${config.taskMandatoryFieldCheckEnabled}")
                                        Boolean taskMandatoryFieldCheckEnabled,
                                        @Value("${config.taskMandatoryFields}") List<String> taskMandatoryFields,
                                        JsonParserUtils jsonParserUtils) {
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.taskMandatoryFieldCheckEnabled = taskMandatoryFieldCheckEnabled;
        this.taskMandatoryFields = taskMandatoryFields;
        this.jsonParserUtils = jsonParserUtils;
    }

    /**
     * Validates the mandatory fields of a task.
     *
     * @param task the task to be validated
     */
    public void validate(TaskResource task) {
        if (taskMandatoryFieldCheckEnabled) {
            log.info("Validating mandatory fields for task {}", task.getTaskId());
            LDValue mandatoryFieldCheckEnabledServices = launchDarklyFeatureFlagProvider.getJsonValue(
                FeatureFlag.WA_MANDATORY_FIELD_CHECK,
                LDValue.of("{\"jurisdictions\": []}")
            );

            if (mandatoryFieldCheckEnabledServices == null) {
                log.warn("Mandatory field check enabled services is null, skipping validation.");
                return;
            }

            JsonNode excludedJurisdictionsArray =
                jsonParserUtils.parseJson(mandatoryFieldCheckEnabledServices.toJsonString(),
                                          MANDATORY_FIELD_CHECK_FLAG_VARIANT.stringValue());
            if (excludedJurisdictionsArray == null) {
                log.warn("Excluded jurisdictions array is null, skipping jurisdiction check.");
                validateTaskMandatoryFields(task);
                return;
            }

            if (!isJurisdictionExcluded(task, excludedJurisdictionsArray)) {
                validateTaskMandatoryFields(task);
            }
        }
    }

    /**
     * Checks if the task's jurisdiction is excluded.
     *
     * @param task the task to be checked
     * @param excludedJurisdictionsArray the array of excluded jurisdictions
     * @return true if the jurisdiction is excluded, false otherwise
     */
    private boolean isJurisdictionExcluded(TaskResource task, JsonNode excludedJurisdictionsArray) {
        for (JsonNode jurisdiction : excludedJurisdictionsArray) {
            if (jurisdiction.asText().equals(task.getJurisdiction())) {
                log.warn("Task {} is excluded from mandatory field check due to jurisdiction being excluded {}",
                         task.getTaskId(), task.getJurisdiction());
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the mandatory fields of a given task.
     *
     * @param task the task to be validated
     * @throws ServiceMandatoryFieldValidationException if any service-specific mandatory fields are null or empty
     * @throws ValidationException if any TM-specific mandatory fields are null or empty
     */
    public void validateTaskMandatoryFields(TaskResource task) {
        List<String> serviceSpecificErrors = new ArrayList<>();
        List<String> tmSpecificErrors = new ArrayList<>();

        for (String field : taskMandatoryFields) {
            try {
                Object fieldValue = PropertyUtils.getProperty(task, field);
                if (isFieldNullOrEmpty(fieldValue)) {
                    addError(field, serviceSpecificErrors, tmSpecificErrors);
                }
                if (ROLE_CATEGORY.value().equals(field) && fieldValue != null && !fieldValue.toString().isBlank()) {
                    if (!RoleCategory.isAllowed(fieldValue.toString())) {
                        addNotAllowedValuesError(field,fieldValue, serviceSpecificErrors, tmSpecificErrors);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot find property value for mandatory field " + field, e);
            }
        }

        if (!serviceSpecificErrors.isEmpty()) {
            throw new ServiceMandatoryFieldValidationException(
                String.join(", ", serviceSpecificErrors.toString()));
        }
        if (!tmSpecificErrors.isEmpty()) {
            throw new ValidationException(String.join(", ", tmSpecificErrors.toString()));
        }
    }

    /**
     * Checks if the given field value is null or empty.
     *
     * @param fieldValue the value of the field to be checked
     * @return true if the field value is null or empty, false otherwise
     */
    private boolean isFieldNullOrEmpty(Object fieldValue) {
        if (fieldValue instanceof WorkTypeResource workTypeResource) {
            return workTypeResource.getId() == null || workTypeResource.getId().isBlank();
        } else if (fieldValue instanceof ExecutionTypeResource executionTypeResource) {
            return executionTypeResource.getExecutionCode() == null
                || executionTypeResource.getExecutionCode().getValue().isBlank();
        }
        return fieldValue == null || fieldValue.toString().isBlank();
    }

    private void addError(String field, List<String> serviceSpecificErrors, List<String> tmSpecificErrors) {
        String errorMessage = field + " cannot be null or empty";
        if (tmSpecificMandatoryFields.contains(field)) {
            tmSpecificErrors.add(errorMessage);
        } else {
            serviceSpecificErrors.add(errorMessage);
        }
    }

    private void addNotAllowedValuesError(String field, Object fieldValue, List<String> serviceSpecificErrors,
                                          List<String> tmSpecificErrors) {
        String errorMessage = field + " value '" + fieldValue + "' is not one of the allowed values";
        if (tmSpecificMandatoryFields.contains(field)) {
            tmSpecificErrors.add(errorMessage);
        } else {
            serviceSpecificErrors.add(errorMessage);
        }

    }
}
