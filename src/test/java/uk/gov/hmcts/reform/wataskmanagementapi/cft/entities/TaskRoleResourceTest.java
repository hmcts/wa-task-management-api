package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

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
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();

        assertPojoMethodsFor(classUnderTest, FieldPredicate.exclude("taskResource"))
            .testing(Method.TO_STRING)
            .areWellImplemented();

        assertPojoMethodsFor(classUnderTest, FieldPredicate.include("roleName", "taskId"))
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();

    }
}
