package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.QueryRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
class MultipleQueryRequestTest {

    @Value("classpath:roleassignment/queryRequest.json")
    private Resource resourceFile;

    @Autowired
    private JacksonTester<MultipleQueryRequest> jacksonTester;

    @Test
    public void testSerializeQueryRequest() throws Exception {

        QueryRequest queryRequest = QueryRequest.builder().roleType(singletonList(RoleType.CASE))
            .roleName(singletonList("tribunal-caseworker"))
            .validAt(LocalDateTime.parse("2020-10-06T17:00:00"))
            .attributes(Map.of(RoleAttributeDefinition.CASE_ID.value(), singletonList("1604584759556245"))).build();
        MultipleQueryRequest multipleQueryRequest = MultipleQueryRequest.builder()
            .queryRequests(singletonList(queryRequest))
            .build();

        JsonContent<MultipleQueryRequest> queryRequestJsonContent = jacksonTester.write(multipleQueryRequest);
        assertThat(queryRequestJsonContent).isEqualToJson(
            FileUtils.readFileToString(resourceFile.getFile(), StandardCharsets.UTF_8)
        );
    }
}
