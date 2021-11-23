package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;

class CamundaObjectMapperTest {

    CamundaObjectMapper camundaObjectMapper;

    @BeforeEach
    public void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();
    }

    @Test
    void should_convert_object_to_camunda_json() {

        CamundaExceptionMessage testObject = new CamundaExceptionMessage("someType", "someMessage");

        String result = camundaObjectMapper.asCamundaJsonString(testObject);

        String expected = "{\"type\":\"someType\",\"message\":\"someMessage\"}";
        assertEquals(expected, result);
    }

    @Test
    void should_convert_object_to_camunda_json_camel_case() {

        CamundaProcessVariables testObject = processVariables()
            .withProcessVariable("caseId", "0000000")
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .build();

        String result = camundaObjectMapper.asCamundaJsonString(testObject);

        String expected = "{\"processVariablesMap\":"
                          + "{\"caseId\":{\"value\":\"0000000\",\"type\":\"String\"},"
                          + "\"dueDate\":{\"value\":\"2020-09-27\",\"type\":\"String\"},"
                          + "\"taskId\":{\"value\":\"wa-task-configuration-api-task\",\"type\":\"String\"},"
                          + "\"group\":{\"value\":\"TCW\",\"type\":\"String\"}}}";
        assertEquals(expected, result);
    }


    @Test
    void should_convert_object_to_json() {

        CamundaExceptionMessage testObject = new CamundaExceptionMessage("someType", "someMessage");

        String result = camundaObjectMapper.asJsonString(testObject);

        String expected = "{\"type\":\"someType\",\"message\":\"someMessage\"}";
        assertEquals(expected, result);
    }

    @Test
    void should_convert_object_to_json_snake_case() {

        CamundaProcessVariables testObject = processVariables()
            .withProcessVariable("caseId", "0000000")
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .build();

        String result = camundaObjectMapper.asJsonString(testObject);

        String expected = "{\"process_variables_map\":"
                          + "{\"caseId\":{\"value\":\"0000000\",\"type\":\"String\"},"
                          + "\"dueDate\":{\"value\":\"2020-09-27\",\"type\":\"String\"},"
                          + "\"taskId\":{\"value\":\"wa-task-configuration-api-task\",\"type\":\"String\"},"
                          + "\"group\":{\"value\":\"TCW\",\"type\":\"String\"}}}";
        assertEquals(expected, result);
    }

    @Test
    void read_value_method_should_return_correct_attributes() {

        String expectedCamundaVariable = "{\n"
                                         + "    \"value\": \"assigned\",\n"
                                         + "    \"type\": \"string\"\n"
                                         + "}";

        CamundaVariable actual = camundaObjectMapper.readValue(expectedCamundaVariable, CamundaVariable.class);
        assertNotNull(actual.getType());
        assertNotNull(actual.getValue());
        assertEquals(actual.getType(), "string");
        assertEquals(actual.getValue(), "assigned");
    }

    @Test
    void should_throw_IllegalArgumentException_when_invalid_object_is_passed() {
        assertThrows(IllegalArgumentException.class, () ->
            camundaObjectMapper.asCamundaJsonString(new Object()));
    }
}
