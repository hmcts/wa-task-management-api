package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

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
            .areWellImplemented();
    }

    @Test
    void equalIsWellImplemented() {
        DecisionTableRequest obj1 = new DecisionTableRequest(
            CamundaValue.stringValue("some case data"),
            CamundaValue.stringValue("{\"caseTypeId\": \"some task type\"}")
        );
        DecisionTableRequest obj2 = new DecisionTableRequest(
            CamundaValue.stringValue("some case data"),
            CamundaValue.stringValue("{\"caseTypeId\": \"some task type\"}")
        );
        DecisionTableRequest obj3 = new DecisionTableRequest(
            CamundaValue.stringValue("some case data"),
            CamundaValue.stringValue("{\"caseTypeId\": \"some task type3\"}")
        );

        assertEquals(obj1, obj2);
        assertNotEquals(obj1, obj3);

        assertEquals(CamundaValue.stringValue("some case data"), obj1.getCaseData());
        assertEquals(CamundaValue.stringValue("{\"caseTypeId\": \"some task type\"}"), obj1.getTaskAttributes());

    }
}
