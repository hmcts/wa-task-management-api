package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class TaskOperationRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = TaskOperationRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }
}
