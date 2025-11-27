package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;

import java.io.IOException;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Transactional//@Testcontainers
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
class RoleAssignmentTest {

    @Autowired
    IntegrationTestUtils integrationTestUtils;

    @Test
    void deserialize_as_expected_for_unknown_values() throws IOException {
        String jsonContent = "{\""
                             + "id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actorIdType\":\"testUnknownValue\","
                             + "\"actorId\":\"someActorId\","
                             + "\"roleType\":\"testUnknownValue\","
                             + "\"roleName\":\"some-role-name\","
                             + "\"classification\":\"testUnknownValue\","
                             + "\"grantType\":\"testUnknownValue\","
                             + "\"roleCategory\":\"testUnknownValue\","
                             + "\"readOnly\":false,"
                             + "\"beginTime\":null,"
                             + "\"endTime\":null,"
                             + "\"created\":null,"
                             + "\"attributes\":{},"
                             + "\"authorisations\":[]"
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final RoleAssignment expected = integrationTestUtils.getObjectMapper()
            .setPropertyNamingStrategy(LOWER_CAMEL_CASE)
            .readValue(jsonContent, RoleAssignment.class);

        RoleAssignment actual = getAssignmentForUnknownValues();

        assertEquals(expected, actual);
    }

    private RoleAssignment getAssignmentForUnknownValues() {
        return new RoleAssignment(
            "00d1ebd4-06ef-4b53-9571-b138981dc8e0",
            ActorIdType.UNKNOWN,
            "someActorId",
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
