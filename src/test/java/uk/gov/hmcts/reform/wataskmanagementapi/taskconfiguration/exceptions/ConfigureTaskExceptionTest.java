package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.Assignment;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class ConfigureTaskExceptionTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = Assignment.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();
    }

}
