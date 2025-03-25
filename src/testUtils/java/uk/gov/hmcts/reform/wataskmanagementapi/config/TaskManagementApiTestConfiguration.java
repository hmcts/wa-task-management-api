package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TaskManagementApiTestConfiguration {
    @Bean
    public RestApiActions restApiActions(@Value("${targets.instance}") String testUrl) {
        return new RestApiActions(testUrl, PropertyNamingStrategies.SNAKE_CASE);
    }

    @Bean
    public RestApiActions camundaApiActions(@Value("${targets.camunda}") String camundaUrl) {
        return new RestApiActions(camundaUrl, PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @Bean
    public RestApiActions workflowApiActions(@Value("${targets.workflow}") String workflowUrl) {
        return new RestApiActions(workflowUrl, PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @Bean
    public RestApiActions launchDarklyActions(@Value("${launch_darkly.url}") String launchDarklyUrl) {
        return new RestApiActions(launchDarklyUrl, PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }
}
