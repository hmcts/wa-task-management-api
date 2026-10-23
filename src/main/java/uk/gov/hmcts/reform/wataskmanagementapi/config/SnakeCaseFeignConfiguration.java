package uk.gov.hmcts.reform.wataskmanagementapi.config;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Configuration
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SnakeCaseFeignConfiguration {

    private final JsonMapper objectMapper;

    @Autowired
    public SnakeCaseFeignConfiguration(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public HttpMessageConverterCustomizer snakeCaseJacksonConverter() {
        return converters -> {
            JacksonJsonHttpMessageConverter jacksonConverter = new JacksonJsonHttpMessageConverter(objectMapper);
            jacksonConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.valueOf(TEXT_PLAIN_VALUE + ";charset=utf-8"),
                APPLICATION_JSON,
                new MediaType("application", "*+json"),
                TEXT_PLAIN));
            converters.add(jacksonConverter);
        };
    }

    @Bean
    @ConditionalOnMissingBean(FeignHttpMessageConverters.class)
    public FeignHttpMessageConverters feignHttpMessageConverters(
        ObjectProvider<ClientHttpMessageConvertersCustomizer> clientCustomizers,
        ObjectProvider<HttpMessageConverterCustomizer> feignCustomizers
    ) {
        return new FeignHttpMessageConverters(clientCustomizers, feignCustomizers);
    }

    @Bean
    @Primary
    public Encoder feignFormEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public Decoder feignDecoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }

    @Bean
    public Encoder feignEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringEncoder(messageConverters);
    }
}
