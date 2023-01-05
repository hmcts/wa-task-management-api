package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Configuration
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SnakeCaseFeignConfiguration {

    private final ObjectMapper objectMapper;

    @Autowired
    public SnakeCaseFeignConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    @Primary
    public Encoder feignFormEncoder(
        ObjectFactory<HttpMessageConverters> messageConverters
    ) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public Decoder feignDecoder() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        jacksonConverter.setSupportedMediaTypes(Arrays.asList(APPLICATION_JSON,
                                                              new MediaType("application", "*+json"),
                                                              new MediaType("text", "plain")));
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }

    @Bean
    public Encoder feignEncoder() {
        HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
        return new SpringEncoder(objectFactory);
    }
}

