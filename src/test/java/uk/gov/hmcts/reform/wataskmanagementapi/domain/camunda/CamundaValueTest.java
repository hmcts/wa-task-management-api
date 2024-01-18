package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CamundaValueTest {

    @Test
    void should_set_properties() {

        CamundaValue<String> testObject = new CamundaValue<>("0000000", "String");

        Assertions.assertEquals("String", testObject.getType());
        Assertions.assertEquals("0000000", testObject.getValue());
    }

    @Test
    void should_return_camunda_value_from_string() {


        CamundaValue<String> testObject = CamundaValue.stringValue("someString");

        Assertions.assertEquals("String", testObject.getType());
        Assertions.assertEquals("someString", testObject.getValue());
    }

    @Test
    void should_return_camunda_value_from_integer() {

        CamundaValue<Integer> testObject = CamundaValue.integerValue(100);

        Assertions.assertEquals("Integer", testObject.getType());
        Assertions.assertEquals(100, testObject.getValue());
    }

    @Test
    void should_return_camunda_value_from_json_string() {

        CamundaValue<String> testObject = CamundaValue.jsonValue("someJson");

        Assertions.assertEquals("json", testObject.getType());
        Assertions.assertEquals("someJson", testObject.getValue());
    }

    @Test
    void should_return_true_when_objects_are_the_same() {
        CamundaValue<String> targetObj = CamundaValue.jsonValue("someJson");

        Assertions.assertEquals(CamundaValue.jsonValue("someJson"), targetObj);
    }

    @Test
    void should_return_false_when_object_is_null() {
        CamundaValue<String> thisObj = CamundaValue.jsonValue("someJson");

        Assertions.assertNotEquals(null, thisObj);
    }

    @Test
    void hashCode_should_not_be_the_same() {

        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        CamundaValue<String> testObject2 = CamundaValue.jsonValue("someOtherValues");
        Assertions.assertNotEquals(testObject1, testObject2);
        Assertions.assertNotEquals(testObject2.hashCode(), testObject1.hashCode());
    }

    @Test
    void hashCode_should_be_equal() {

        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        CamundaValue<String> testObject2 = CamundaValue.jsonValue("someJson");
        Assertions.assertEquals(testObject1, testObject2);
        Assertions.assertEquals(testObject2.hashCode(), testObject1.hashCode());
    }

    @Test
    void should_convert_value_to_string() {
        CamundaValue<String> testObject1 = CamundaValue.jsonValue("someJson");
        Assertions.assertEquals("CamundaValue(value=someJson, type=json)", testObject1.toString());
    }
}
