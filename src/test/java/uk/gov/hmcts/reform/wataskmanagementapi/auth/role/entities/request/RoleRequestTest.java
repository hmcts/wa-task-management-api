package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

public class RoleRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .areWellImplemented();
    }
}
