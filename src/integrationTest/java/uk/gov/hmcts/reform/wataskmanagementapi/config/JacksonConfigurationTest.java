package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JacksonConfigurationTest {

    @Autowired
    IntegrationTestUtils integrationTestUtils;

    @Test
    void default_object_mapper_should_read_snake_case() throws IOException {

        String jsonContent = "{"
                             + "\"id\":\"00d1ebd4-06ef-4b53-9571-b138981dc8e0\","
                             + "\"actor_id_type\":\"IDAM\","
                             + "\"actor_id\":\"someActorId\""
                             + "}";

        //ObjectMapper has default configuration as set in JacksonConfiguration.class
        final ObjectMapperTestObject actual =
            integrationTestUtils.getObjectMapper().readValue(jsonContent, ObjectMapperTestObject.class);

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
            integrationTestUtils.getObjectMapper().readValue(jsonContent, ObjectMapperTestObject.class);

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
