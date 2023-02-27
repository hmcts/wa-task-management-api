package uk.gov.hmcts.reform.wataskmanagementapi.domain.task;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class WarningValuesTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = WarningValues.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

    @Test
    void deserializeTest() {

        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                        + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";
        WarningValues warningValues = new WarningValues(values);
        assertNotNull(warningValues);
        assertEquals(2,warningValues.getValues().size());
        assertEquals("Code1",warningValues.getValues().get(0).getWarningCode());
        assertEquals("Text1",warningValues.getValues().get(0).getWarningText());
        assertEquals("Code2",warningValues.getValues().get(1).getWarningCode());
        assertEquals("Text2",warningValues.getValues().get(1).getWarningText());
    }
}
