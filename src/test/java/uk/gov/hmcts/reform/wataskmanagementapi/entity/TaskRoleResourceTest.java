package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.FieldPredicate;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskRoleResourceTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskRoleResource.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.SETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();

        assertPojoMethodsFor(classUnderTest, FieldPredicate.exclude("taskResource", "authorizations"))
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }
}
