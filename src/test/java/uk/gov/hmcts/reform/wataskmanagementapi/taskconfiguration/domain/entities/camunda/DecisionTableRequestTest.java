package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class DecisionTableRequestTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = DecisionTableRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            //todo: check here
            //.testing(Method.EQUALS)
            //.testing(Method.HASH_CODE)
            .areWellImplemented();
    }

    @Test
    void equalIsWellImplemented() {
        DecisionTableRequest obj1 = new DecisionTableRequest(
            CamundaValue.stringValue("some case data"),
            CamundaValue.stringValue("some task type")
        );

        DecisionTableRequest obj2 = new DecisionTableRequest(
            CamundaValue.stringValue("some other case data"),
            CamundaValue.stringValue("some other task type")
        );

        DecisionTableRequest obj3 = new DecisionTableRequest(
            CamundaValue.stringValue("some other case data"),
            CamundaValue.stringValue("some other task type")
        );

        assertNotEquals(obj1, obj2);
        assertEquals(obj2, obj3);
    }

}
