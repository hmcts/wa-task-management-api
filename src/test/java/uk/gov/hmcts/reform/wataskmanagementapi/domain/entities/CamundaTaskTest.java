package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class CamundaTaskTest {


    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = CamundaTask.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
