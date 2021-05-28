package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import java.util.ArrayList;
import java.util.List;

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
        Warning warning = new Warning("Code1","Text");
        Warning warning2 = new Warning("Code2","Text1");
        List<Warning> list = new ArrayList<>();
        list.add(warning);
        list.add(warning2);

        WarningValues warningValues = new WarningValues(new WarningValues(list).toString());
        assertNotNull(warningValues);
    }
}
