package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InitiateTaskRequestTest.MinimalTestConfig.class)
@TestInstance(PER_CLASS)
class InitiateTaskRequestTest {

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
    ObjectMapper objectMapper;

    @Test
    void given_snake_case_initiate_body_request_when_deserializes_it_keeps_attribute_list_and_operation_fields()
        throws JsonProcessingException {
        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "aTaskType",
            TASK_NAME.value(), "aTaskName",
            CASE_ID.value(), "1634748573864804",
            TASK_TITLE.value(), "A test task"
        );

        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, taskAttributes);

        String expectedInitiateBodyRequest = objectMapper.writeValueAsString(initiateTaskRequest);

        InitiateTaskRequestMap actual = objectMapper.readValue(
            expectedInitiateBodyRequest,
            InitiateTaskRequestMap.class
        );

        assertThat(actual.getTaskAttributes()).isNotNull();
        assertThat(actual.getTaskAttributes()).isEqualTo(taskAttributes);
        assertThat(actual.getOperation()).isEqualTo(INITIATION);
    }

}
