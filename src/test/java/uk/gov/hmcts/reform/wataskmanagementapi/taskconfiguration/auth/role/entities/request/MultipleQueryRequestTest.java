package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.MultipleQueryRequest;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class MultipleQueryRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = MultipleQueryRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

}
