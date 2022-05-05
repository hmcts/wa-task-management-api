package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.AssigneeRequest;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class AssigneeRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = AssigneeRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

}
