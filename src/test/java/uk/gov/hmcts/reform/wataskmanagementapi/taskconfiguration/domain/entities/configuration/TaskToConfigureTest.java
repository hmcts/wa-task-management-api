package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskToConfigure;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskToConfigureTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskToConfigure.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

}
