package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.FieldPredicate.exclude;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskResourceTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskResource.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.SETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();

        assertPojoMethodsFor(classUnderTest, exclude("taskRoleResources"))
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }

}
