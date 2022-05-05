package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class ConfigureTaskExceptionTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = ConfigureTaskException.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();
    }

}
