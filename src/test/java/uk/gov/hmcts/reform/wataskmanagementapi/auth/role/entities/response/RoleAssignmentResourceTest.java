package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class RoleAssignmentResourceTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleAssignmentResource.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }
}
