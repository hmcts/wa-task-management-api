package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.io.IOException;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RoleAssignmentTest.MinimalTestConfig.class)
@TestInstance(PER_CLASS)
class RoleAssignmentTest {

    @TestConfiguration
    static class MinimalTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new Jdk8Module());
            return mapper;
        }
    }

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

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final RoleAssignment expected = objectMapper
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
