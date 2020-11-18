package uk.gov.hmcts.reform.wataskmanagementapi.controllers.assignTaskController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class AssigneeRequestTest {

    public static final String ID = "37d4eab7-e14c-404e-8cd1-55cd06b2fc06";
    public static final String USER_ID = "{\"userId\": \"" + ID + "\"}";

    @Autowired
    private JacksonTester<AssigneeRequest> jacksonTester;
    private final AssigneeRequest assigneeRequest = new AssigneeRequest(ID);

    @Test
    public void testSerializeAssigneeRequest() throws IOException {
        JsonContent<AssigneeRequest> assigneeRequestJsonContent = jacksonTester.write(assigneeRequest);

        assertThat(assigneeRequestJsonContent).isEqualToJson(USER_ID);
    }

    @Test
    public void testDeserializeAssignedRequest() throws IOException {
        ObjectContent<AssigneeRequest> actualAssigneeRequest = jacksonTester.parse(USER_ID);

        actualAssigneeRequest.assertThat().isEqualTo(assigneeRequest);
    }
}
