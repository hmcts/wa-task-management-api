package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JacksonConfigurationTest.MinimalTestConfig.class)
@TestInstance(PER_CLASS)
public class JacksonConfigurationTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    @TestConfiguration
    static class MinimalTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
            mapper.configure(READ_ENUMS_USING_TO_STRING, true);
            mapper.configure(WRITE_ENUMS_USING_TO_STRING, true);
            mapper.setDateFormat(df);
            mapper.registerModule(new ParameterNamesModule());
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new ConstraintViolationProblemModule());
            return mapper;
        }
    }

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    void default_object_mapper_should_read_snake_case() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"IDAM\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual =
            objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

        assertEquals("00d1ebd4-06ef-4b53-9571-b138981dc8e0", actual.getId());
        assertEquals(ActorIdType.IDAM, actual.getActorIdType());
        assertEquals("someActorId", actual.getActorId());
    }

    @Test
    void default_object_mapper_should_convert_enums_to_defaults() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"testUnkownValue\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual =
            objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

        assertEquals("00d1ebd4-06ef-4b53-9571-b138981dc8e0", actual.getId());
        assertEquals(ActorIdType.UNKNOWN, actual.getActorIdType());
        assertEquals("someActorId", actual.getActorId());
    }

    @Builder
    @Getter
    private static class ObjectMapperTestObject {
        private final String id;
        private final ActorIdType actorIdType;
        private final String actorId;

    }

}
