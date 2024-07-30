package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import com.launchdarkly.sdk.LDValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.ArrayList;
import java.util.List;
import javax.validation.ValidationException;

@Slf4j
public class TaskMandatoryFieldsValidator {

    @Autowired
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private final Boolean  isMandatoryFieldCheckEnabled;
    private final List<String>  mandatoryTaskFields;

    public static final LDValue DEFAULT_VARIANT_VALUE = LDValue.of("jurisdiction");

    @Autowired
    public TaskMandatoryFieldsValidator(@Value("${config.mandatoryTaskFieldCheckEnabled}")
                                            Boolean mandatoryTaskFieldCheckEnabled,
                                        @Value("${config.mandatoryTaskFields}") List<String> mandatoryTaskFields) {
        this.mandatoryTaskFields = mandatoryTaskFields;
        this.isMandatoryFieldCheckEnabled = mandatoryTaskFieldCheckEnabled;
    }

    public void validate(TaskResource task) {
        log.info("Validating mandatory fields for task {}", task.getTaskId());

        LDValue mandatoryFieldCheckEnabledServices = launchDarklyFeatureFlagProvider.getJsonValue(
            FeatureFlag.WA_MANDATORY_FIELD_CHECK,
            "ccd-case-disposer",
            "ccd-case-disposer@hmcts.net",
            DEFAULT_VARIANT_VALUE
        );
        log.info("mandatoryFieldCheckEnabledServices {}", mandatoryFieldCheckEnabledServices);
        if (isMandatoryFieldCheckEnabled && mandatoryFieldCheckEnabledServices.toJsonString()
            .contains(task.getJurisdiction())) {
            List<String> errors = new ArrayList<>();

            for (String field : mandatoryTaskFields) {
                try {
                    Object value = PropertyUtils.getProperty(task, field);
                    if (value == null) {
                        errors.add(field + " cannot be null");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error checking mandatory fields", e);
                }
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(String.join(", ", errors));
            }
        }
    }
}
