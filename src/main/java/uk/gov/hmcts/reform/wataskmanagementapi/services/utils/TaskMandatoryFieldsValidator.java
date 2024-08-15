package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.sdk.LDValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.ArrayList;
import java.util.List;
import javax.validation.ValidationException;

/**
 * Service to validate mandatory fields of a task.
 */
@Slf4j
@Service
@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public class TaskMandatoryFieldsValidator {

    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final Boolean isMandatoryFieldCheckEnabled;
    private final List<String> taskMandatoryFields;
    public static final LDValue MANDATORY_FIELD_CHECK_FLAG_VARIANT = LDValue.of("jurisdictions");

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
                                        @Value("${config.taskMandatoryFields}") List<String> taskMandatoryFields) {
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.isMandatoryFieldCheckEnabled = taskMandatoryFieldCheckEnabled;
        this.taskMandatoryFields = taskMandatoryFields;
    }

    /**
     * Validates the mandatory fields of a task.
     *
     * @param task the task to be validated
     */
    public void validate(TaskResource task) {
        log.info("Validating mandatory fields for task {}", task.getTaskId());

        if (isMandatoryFieldCheckEnabled) {
            LDValue mandatoryFieldCheckEnabledServices = launchDarklyFeatureFlagProvider.getJsonValue(
                FeatureFlag.WA_MANDATORY_FIELD_CHECK,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net",
                LDValue.of("{\"jurisdictions\": []}")
            );

            if (mandatoryFieldCheckEnabledServices == null) {
                log.warn("Mandatory field check enabled services is null, skipping validation.");
                return;
            }

            JsonNode excludedJurisdictionsArray = parseJson(mandatoryFieldCheckEnabledServices.toJsonString());
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
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a JSON string and returns the JsonNode for the mandatory field check flag variant.
     *
     * @param jsonString the JSON string to be parsed
     * @return the JsonNode for the mandatory field check flag variant
     * @throws IllegalArgumentException if the JSON string cannot be parsed
     */
    public JsonNode parseJson(String jsonString) {
        try {
            return new ObjectMapper().readTree(jsonString).get(MANDATORY_FIELD_CHECK_FLAG_VARIANT.stringValue());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mandatory flag jurisdictions mapping issue.", e);
        }
    }

    /**
     * Validates the mandatory fields of a task.
     *
     * @param task the task to be validated
     * @throws ValidationException if any mandatory field is missing or invalid
     * @throws IllegalArgumentException if a property value cannot be found
     */
    public void validateTaskMandatoryFields(TaskResource task) {
        List<String> errors = new ArrayList<>();
        for (String field : taskMandatoryFields) {
            try {
                if (PropertyUtils.getProperty(task, field) == null
                    || PropertyUtils.getProperty(task, field).toString().isBlank()) {
                    errors.add(field + " cannot be null or empty");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot find property value for field " + field, e);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(", ", errors));
        }
    }
}
