package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "IDAM_URL=https://idam-api.aat.platform.hmcts.net",
    "OPEN_ID_IDAM_URL=https://idam-web-public.aat.platform.hmcts.net",
    "CCD_URL=http://ccd-data-store-api-aat.service.core-compute-aat.internal"
})
@Testcontainers
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SpringBootIntegrationBaseTest {

    @Container
    private static PostgreSQLContainer<?> POSTGRES_SQL_CONTAINER = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:11"))
        .withDatabaseName("cft_db_test")
        .withUsername("postgres")
        .withPassword("pass");

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @LocalServerPort
    protected int port;

    protected String asJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    public void db_container_is_running() {
        assertTrue(POSTGRES_SQL_CONTAINER.isRunning());
    }
}
