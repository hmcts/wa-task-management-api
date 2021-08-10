package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.problem.ProblemModule;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        // Set default date to RFC3339 standards
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
        return new Jackson2ObjectMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .featuresToEnable(READ_ENUMS_USING_TO_STRING)
            .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
            .dateFormat(df)
            .modules(
                new ParameterNamesModule(),
                new JavaTimeModule(),
                new Jdk8Module(),
                new ProblemModule()
            );
    }

    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        Jackson2ObjectMapperBuilder builder = jackson2ObjectMapperBuilder();
        return new MappingJackson2HttpMessageConverter(builder.build());
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = jackson2ObjectMapperBuilder();
        return builder.createXmlMapper(false).build();
    }

}
