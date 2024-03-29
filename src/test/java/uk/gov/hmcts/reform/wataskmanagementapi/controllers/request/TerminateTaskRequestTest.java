package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TerminateTaskRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TerminateTaskRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }
}
