package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class ActorIdTypeTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = ActorIdType.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .areWellImplemented();
    }

}
