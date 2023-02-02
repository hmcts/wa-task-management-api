package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class PermissionsDmnEvaluationResponseTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = PermissionsDmnEvaluationResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .areWellImplemented();
    }

}
