package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class SearchParameterBooleanTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = SearchParameterBoolean.class;
//The test for getter won't work, whilst the value we receive from the API contains a value,
// we have written the interface to return values, as it would be apply to any implementation of a Search Parameter
        assertPojoMethodsFor(classUnderTest)
//            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
