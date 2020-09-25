package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class CamundaValueTest {

    @Test
    void should_set_properties() {

        CamundaValue<String> testObject = new CamundaValue<>("0000000", "String");

        assertEquals("String", testObject.getType());
        assertEquals("0000000", testObject.getValue());
    }

    @Test
    void should_return_camunda_value_from_string() {


        CamundaValue<String> testObject = CamundaValue.stringValue("someString");

        assertEquals("String", testObject.getType());
        assertEquals("someString", testObject.getValue());
    }

    @Test
    void should_return_camunda_value_from_json_string() {

        CamundaValue<String> testObject = CamundaValue.jsonValue("someJson");

        assertEquals("json", testObject.getType());
        assertEquals("someJson", testObject.getValue());
    }
}
