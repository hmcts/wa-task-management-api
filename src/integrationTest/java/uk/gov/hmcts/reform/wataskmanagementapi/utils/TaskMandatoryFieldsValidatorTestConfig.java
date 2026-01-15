package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.JsonParserUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.util.List;

@TestConfiguration
public class TaskMandatoryFieldsValidatorTestConfig {

    @Bean
    public JsonParserUtils jsonParserUtils(ObjectMapper objectMapper) {
        return new JsonParserUtils(objectMapper);
    }

    @Bean
    public LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider() {
        return Mockito.mock(LaunchDarklyFeatureFlagProvider.class);
    }

    @Bean
    public TaskMandatoryFieldsValidator taskMandatoryFieldsValidator(
            LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
            JsonParserUtils jsonParserUtils) {
        return new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider,
            true,  // taskMandatoryFieldCheckEnabled
            List.of("taskName", "taskId", "taskType", "dueDateTime", "state",
                "securityClassification", "title", "majorPriority", "minorPriority",
                "executionTypeCode", "caseId", "caseTypeId", "caseCategory", "caseName",
                "jurisdiction", "region", "location", "created", "roleCategory", "workTypeResource"),
            jsonParserUtils
        );
    }
}
