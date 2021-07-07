package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.FieldPredicate;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

public class TasksTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = Tasks.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();

        assertPojoMethodsFor(classUnderTest, FieldPredicate.exclude("taskRoles"))
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }

}
