package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.JsonParserUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.util.Arrays;
import java.util.List;

@TestConfiguration
@PropertySource(value = "classpath:application-integration.yaml", factory = YamlPropertySourceFactory.class)
public class TaskMandatoryFieldsValidatorTestConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

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
            @Value("${config.taskMandatoryFieldCheckEnabled}") Boolean taskMandatoryFieldCheckEnabled,
            @Value("${config.taskMandatoryFields}") String taskMandatoryFieldsString,
            JsonParserUtils jsonParserUtils) {
        List<String> taskMandatoryFields = Arrays.asList(taskMandatoryFieldsString.split(","));
        return new TaskMandatoryFieldsValidator(
            launchDarklyFeatureFlagProvider,
            taskMandatoryFieldCheckEnabled,
            taskMandatoryFields,
            jsonParserUtils
        );
    }
}
