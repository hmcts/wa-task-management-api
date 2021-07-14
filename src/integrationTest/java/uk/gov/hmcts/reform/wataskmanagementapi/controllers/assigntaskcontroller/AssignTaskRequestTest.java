package uk.gov.hmcts.reform.wataskmanagementapi.controllers.assigntaskcontroller;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
class AssignTaskRequestTest {

    public static final String ID = "37d4eab7-e14c-404e-8cd1-55cd06b2fc06";
    public static final String USER_ID = "{\"user_id\": \"" + ID + "\"}";
    private final AssignTaskRequest assignTaskRequest = new AssignTaskRequest(ID);
    @Autowired
    private JacksonTester<AssignTaskRequest> jacksonTester;

    @Test
    void testSerializeAssigneeRequest() throws IOException {
        JsonContent<AssignTaskRequest> assigneeRequestJsonContent = jacksonTester.write(assignTaskRequest);

        assertThat(assigneeRequestJsonContent).isEqualToJson(USER_ID);
    }

    @Test
    void testDeserializeAssignedRequest() throws IOException {
        ObjectContent<AssignTaskRequest> actualAssigneeRequest = jacksonTester.parse(USER_ID);

        actualAssigneeRequest.assertThat().isEqualTo(assignTaskRequest);
    }
}
