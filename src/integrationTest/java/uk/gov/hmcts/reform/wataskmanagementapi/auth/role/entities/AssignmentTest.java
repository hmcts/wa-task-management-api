package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserialize_as_expected_for_unknown_values() throws IOException {
        String jsonContent = "{\""
            + "id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
            + "\"actorIdType\":\"actorIdType\","
            + "\"actorId\":\"someactorId\","
            + "\"roleType\":\"roleType\","
            + "\"roleName\":\"some-role-name\","
            + "\"classification\":\"classification\","
            + "\"grantType\":\"grantType\","
            + "\"roleCategory\":\"roleCategory\","
            + "\"readOnly\":false,"
            + "\"beginTime\":null,"
            + "\"endTime\":null,"
            + "\"created\":null,"
            + "\"attributes\":{},"
            + "\"authorisations\":[]"
            + "}";

        final Assignment expected = objectMapper.readValue(jsonContent, Assignment.class);

        Assignment actual = getAssignmentForUnknownValues();

        assertEquals(expected, actual);
    }

    private Assignment getAssignmentForUnknownValues() {
        return new Assignment(
            "00d1ebd4-06ef-4b53-9571-b138981dc8e0",
            ActorIdType.UNKNOWN,
            "someactorId",
            RoleType.UNKNOWN,
            "some-role-name",
            Classification.UNKNOWN,
            GrantType.UNKNOWN,
            RoleCategory.UNKNOWN,
            false,
            null,
            null,
            null,
            emptyMap(),
            emptyList()
        );
    }
}
