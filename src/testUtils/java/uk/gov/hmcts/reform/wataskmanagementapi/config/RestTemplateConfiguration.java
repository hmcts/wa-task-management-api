package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestOperations restOperations(
        JsonMapper objectMapper
    ) {
        return restTemplate(objectMapper);
    }

    @Bean
    public RestTemplate restTemplate(
        JsonMapper objectMapper
    ) {
        RestTemplate restTemplate = new RestTemplate();
        JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter =
            new JacksonJsonHttpMessageConverter(objectMapper);
        //Remove default
        restTemplate.getMessageConverters().removeIf(JacksonJsonHttpMessageConverter.class::isInstance);
        //Add autowired message converters as defined in JacksonConfiguration.java
        restTemplate.getMessageConverters().add(jacksonJsonHttpMessageConverter);

        return restTemplate;
    }

}
