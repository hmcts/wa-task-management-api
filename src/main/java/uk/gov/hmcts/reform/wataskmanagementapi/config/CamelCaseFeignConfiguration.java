package uk.gov.hmcts.reform.wataskmanagementapi.config;

import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Configuration
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CamelCaseFeignConfiguration {

    private final JsonMapper objectMapper;

    @Autowired
    public CamelCaseFeignConfiguration(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public HttpMessageConverterCustomizer camelCaseJacksonConverter() {
        return converters -> {
            JacksonJsonHttpMessageConverter jacksonConverter =
                new JacksonJsonHttpMessageConverter(camelCasedObjectMapper());
            jacksonConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.valueOf(TEXT_PLAIN_VALUE + ";charset=utf-8"),
                APPLICATION_JSON,
                new MediaType("application", "*+json"),
                TEXT_PLAIN));
            // Keep raw String request bodies (for example role assignment JSON) ahead of Jackson,
            // while using this mapper before the generic Jackson converter for object bodies.
            int insertionIndex = 0;
            while (insertionIndex < converters.size()
                && !(converters.get(insertionIndex) instanceof StringHttpMessageConverter)) {
                insertionIndex++;
            }
            if (insertionIndex < converters.size()) {
                insertionIndex++;
            }
            converters.add(insertionIndex, jacksonConverter);
        };
    }

    @Bean
    public Decoder feignDecoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }

    @Bean
    public Encoder feignEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringEncoder(messageConverters);
    }

    public JsonMapper camelCasedObjectMapper() {
        //Override naming strategy for this class only
        return objectMapper.rebuild()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build();
    }
}
