package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    void should_return_true_when_objects_reference_is_same() {
        CamundaValue<String> targetObj = CamundaValue.jsonValue("someJson");
        CamundaValue<String> thisObj = targetObj;

        assertTrue(thisObj.equals(targetObj));
    }

    @Test
    void should_return_false_when_object_is_null() {
        CamundaValue<String> thisObj = CamundaValue.jsonValue("someJson");

        assertFalse(thisObj.equals(null));
    }

    @Test
    void hashCode_should_not_be_the_same() {

        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        CamundaValue<String> testObject2 = CamundaValue.jsonValue("someOtherValues");
        assertNotEquals(testObject1, testObject2);
        assertNotEquals(testObject2.hashCode(), testObject1.hashCode());
    }

    @Test
    void hashCode_should_be_equal() {

        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        CamundaValue<String> testObject2 = CamundaValue.jsonValue("someJson");
        assertEquals(testObject1, testObject2);
        assertEquals(testObject2.hashCode(), testObject1.hashCode());
    }

    @Test
    void should_convert_value_to_string() {
        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        assertEquals("CamundaValue{value='someJson', type='json'}", testObject1.toString());
    }
}
