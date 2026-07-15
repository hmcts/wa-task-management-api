package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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
    public TaskMandatoryFieldsValidator taskMandatoryFieldsValidator(
        @Value("${config.taskMandatoryFields}") String taskMandatoryFieldsString) {
        List<String> taskMandatoryFields = Arrays.asList(taskMandatoryFieldsString.split(","));
        return new TaskMandatoryFieldsValidator(taskMandatoryFields);
    }
}
