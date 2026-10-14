package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.JacksonConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchRequestCustomDeserializer;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JacksonConfiguration.class, SearchRequestCustomDeserializer.class})
class RoleAssignmentTest {

    @Autowired
    ObjectMapper objectMapper;

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

        ObjectMapper camelCaseMapper = objectMapper.rebuild()
            .propertyNamingStrategy(LOWER_CAMEL_CASE)
            .build();

        final RoleAssignment expected = camelCaseMapper
            .readValue(jsonContent, RoleAssignment.class);

        RoleAssignment actual = getAssignmentForUnknownValues();

        assertEquals(expected, actual);
    }

    @Test
    void should_round_trip_role_assignment_values_using_camel_case() throws IOException {
        RoleAssignment expected = new RoleAssignment(
            "00d1ebd4-06ef-4b53-9571-b138981dc8e0",
            ActorIdType.IDAM,
            "someActorId",
            RoleType.CASE,
            "tribunal-caseworker",
            Classification.PUBLIC,
            GrantType.STANDARD,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            null,
            null,
            null,
            emptyMap(),
            emptyList()
        );
        ObjectMapper camelCaseMapper = objectMapper.rebuild()
            .propertyNamingStrategy(LOWER_CAMEL_CASE)
            .build();

        String json = camelCaseMapper.writeValueAsString(expected);

        assertTrue(json.contains("\"actorIdType\":\"IDAM\""));
        assertTrue(json.contains("\"roleType\":\"CASE\""));
        assertTrue(json.contains("\"roleCategory\":\"LEGAL_OPERATIONS\""));
        assertFalse(json.contains("actor_id_type"));
        assertFalse(json.contains("role_type"));
        assertFalse(json.contains("\"beginTime\""));

        RoleAssignment actual = camelCaseMapper.readValue(json, RoleAssignment.class);

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
