package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
@Configuration
public class CamundaFeignConfiguration {

    @Bean
    public Decoder feignDecoder() {
        return new JacksonDecoder(camundaObjectMapper());
    }

    @Bean
    public Encoder feignEncoder() {
        return new JacksonEncoder(camundaObjectMapper());
    }

    public ObjectMapper camundaObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
