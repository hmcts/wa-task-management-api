package uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class SearchParameterBooleanTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = SearchParameterBoolean.class;
        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
