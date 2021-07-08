package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.roleassignment;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
class QueryRequestTest {

    @Autowired
    private JacksonTester<QueryRequest> jacksonTester;

    @Test
    public void testSerializeQueryRequest() throws Exception {
        QueryRequest queryRequest = QueryRequest.builder()
            .roleType(Collections.singletonList(RoleType.CASE))
            .roleName(Collections.singletonList("tribunal-caseworker"))
            .validAt(LocalDateTime.parse("2020-10-06T17:00:00"))
            .attributes(Map.of(RoleAttributeDefinition.CASE_ID.value(), Collections.singletonList("1604584759556245")))
            .build();

        JsonContent<QueryRequest> queryRequestJsonContent = jacksonTester.write(queryRequest);

        assertThat(queryRequestJsonContent).isEqualToJson("queryRequest.json");
    }
}
