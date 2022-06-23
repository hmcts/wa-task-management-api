package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskResourceSummaryTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskResourceSummary.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.SETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();
    }
}
