package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

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

@Slf4j
@Service
@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public class TaskMandatoryFieldsValidator {

    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final Boolean isMandatoryFieldCheckEnabled;
    private final List<String> mandatoryTaskFields;
    public static final LDValue MANDATORY_FLAG_VARIANT = LDValue.of("jurisdictions");

    @Autowired
    public TaskMandatoryFieldsValidator(LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                        @Value("${config.mandatoryTaskFieldCheckEnabled}")
                                        Boolean mandatoryTaskFieldCheckEnabled,
                                        @Value("${config.mandatoryTaskFields}") List<String> mandatoryTaskFields) {
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.isMandatoryFieldCheckEnabled = mandatoryTaskFieldCheckEnabled;
        this.mandatoryTaskFields = mandatoryTaskFields;
    }

    public void validate(TaskResource task) {
        log.info("Validating mandatory fields for task {}", task.getTaskId());

        if (isMandatoryFieldCheckEnabled) {
            LDValue mandatoryFieldCheckEnabledServices = launchDarklyFeatureFlagProvider.getJsonValue(
                FeatureFlag.WA_MANDATORY_FIELD_CHECK,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net",
                LDValue.of("{\"jurisdictions\": []}")
            );

            if (mandatoryFieldCheckEnabledServices != null) {
                JsonNode jurisdictionArray = parseJson(mandatoryFieldCheckEnabledServices.toJsonString());
                validateTaskFields(task, jurisdictionArray);
            }
        }
    }

    private JsonNode parseJson(String jsonString) {
        try {
            return new ObjectMapper().readTree(jsonString).get(MANDATORY_FLAG_VARIANT.stringValue());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mandatory flag jurisdictions mapping issue.", e);
        }
    }

    private void validateTaskFields(TaskResource task, JsonNode jurisdictionArray) {
        List<String> errors = new ArrayList<>();
        for (JsonNode jurisdiction : jurisdictionArray) {
            if (jurisdiction.asText().equals(task.getJurisdiction())) {
                for (String field : mandatoryTaskFields) {
                    try {
                        if (PropertyUtils.getProperty(task, field) == null) {
                            errors.add(field + " cannot be null");
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
    }
}
