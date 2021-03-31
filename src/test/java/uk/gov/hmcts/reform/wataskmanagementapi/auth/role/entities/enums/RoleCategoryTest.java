package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class RoleCategoryTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = RoleCategory.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .areWellImplemented();
    }


}
