package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class RoleAssignmentRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleAssignmentRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }
}
