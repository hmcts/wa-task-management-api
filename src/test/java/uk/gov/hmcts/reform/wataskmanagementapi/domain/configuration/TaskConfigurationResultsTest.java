package uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskConfigurationResultsTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskConfigurationResults.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.SETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

}
