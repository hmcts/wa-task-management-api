package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonConfigurationTest extends SpringBootIntegrationBaseTest {

    @Test
    void default_object_mapper_should_read_snake_case() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"IDAM\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual = objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

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
        final ObjectMapperTestObject actual = objectMapper.readValue(jsonContent, ObjectMapperTestObject.class);

        assertEquals("00d1ebd4-06ef-4b53-9571-b138981dc8e0", actual.getId());
        assertEquals(ActorIdType.UNKNOWN, actual.getActorIdType());
        assertEquals("someActorId", actual.getActorId());
    }

    @Builder
    @Getter
    private static class ObjectMapperTestObject {
        private final String id;
        private final ActorIdType actorIdType;
        private final String actorId;

    }

}
