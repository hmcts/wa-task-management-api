package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class SensitiveTaskEventLogTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = SensitiveTaskEventLog.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.SETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();
    }

}

