package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestOperations restOperations(
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
    ) {
        return restTemplate(mappingJackson2HttpMessageConverter);
    }

    @Bean
    public RestTemplate restTemplate(
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
    ) {
        RestTemplate restTemplate = new RestTemplate();
        //Remove default
        restTemplate.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        //Add autowired message converters as defined in JacksonConfiguration.java
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);

        return restTemplate;
    }

}
