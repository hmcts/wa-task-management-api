package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchRequestCustomDeserializer;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public JsonMapper objectMapper(SearchRequestCustomDeserializer searchRequestCustomDeserializer) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SearchParameter.class, searchRequestCustomDeserializer);

        return JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(value -> value
                .withValueInclusion(JsonInclude.Include.NON_ABSENT)
                .withContentInclusion(JsonInclude.Include.NON_ABSENT))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
            .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .defaultDateFormat(dateFormat)
            .addModule(module)
            .build();
    }

}
