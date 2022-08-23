package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import java.util.List;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class InitiateTaskRequestTest {

    @Test
    void isWellImplemented() {
        final List<Class<?>> classUnderTests = List.of(InitiateTaskRequest.class, InitiateTaskRequestNew.class);

        classUnderTests
            .forEach(aClass -> assertPojoMethodsFor(aClass)
                .testing(Method.GETTER)
                .testing(Method.CONSTRUCTOR)
                .testing(Method.TO_STRING)
                .testing(Method.EQUALS)
                .testing(Method.HASH_CODE)
                .areWellImplemented());
    }
}
