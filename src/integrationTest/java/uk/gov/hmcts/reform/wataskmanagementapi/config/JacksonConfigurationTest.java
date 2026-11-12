package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchRequestCustomDeserializer;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JacksonConfiguration.class, SearchRequestCustomDeserializer.class})
public class JacksonConfigurationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void default_object_mapper_should_read_snake_case() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"IDAM\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual =
            objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

        assertEquals("00d1ebd4-06ef-4b53-9571-b138981dc8e0", actual.getId());
        assertEquals(ActorIdType.IDAM, actual.getActorIdType());
        assertEquals("someActorId", actual.getActorId());
    }

    @Test
    void default_object_mapper_should_convert_enums_to_defaults() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"testUnkownValue\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual =
            objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

        assertEquals("00d1ebd4-06ef-4b53-9571-b138981dc8e0", actual.getId());
        assertEquals(ActorIdType.UNKNOWN, actual.getActorIdType());
        assertEquals("someActorId", actual.getActorId());
    }

    @Test
    void default_object_mapper_should_round_trip_the_json_contract() throws IOException {
        ObjectMapperTestObject expected = ObjectMapperTestObject.builder()
            .id("00d1ebd4-06ef-4b53-9571-b138981dc8e0")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someActorId")
            .createdAt(new Date(1705314030000L))
            .build();

        String json = objectMapper.writeValueAsString(expected);

        assertTrue(json.contains("\"actor_id_type\":\"IDAM\""));
        assertTrue(json.contains("\"actor_id\":\"someActorId\""));
        assertTrue(json.contains("\"created_at\":\"2024-01-15T10:20:30+0000\""));
        assertFalse(json.contains("\"actorIdType\""));
        assertFalse(json.contains("\"optional_value\""));

        ObjectMapperTestObject actual = objectMapper.readValue(json, ObjectMapperTestObject.class);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getActorIdType(), actual.getActorIdType());
        assertEquals(expected.getActorId(), actual.getActorId());
        assertEquals(expected.getCreatedAt(), actual.getCreatedAt());
        assertNull(actual.getOptionalValue());
    }

    @Builder
    @Getter
    private static class ObjectMapperTestObject {
        private final String id;
        private final ActorIdType actorIdType;
        private final String actorId;
        private final String optionalValue;
        private final Date createdAt;

    }

}
