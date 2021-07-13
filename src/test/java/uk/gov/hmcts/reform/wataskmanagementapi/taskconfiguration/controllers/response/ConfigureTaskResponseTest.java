package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class ConfigureTaskResponseTest {

    private final Class<ConfigureTaskResponse> classToTest = ConfigureTaskResponse.class;

    @Test
    void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

}
