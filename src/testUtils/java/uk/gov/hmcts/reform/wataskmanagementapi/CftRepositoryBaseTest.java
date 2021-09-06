package uk.gov.hmcts.reform.wataskmanagementapi;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("db-lock")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "IDAM_URL=https://idam-api.aat.platform.hmcts.net",
    "OPEN_ID_IDAM_URL=https://idam-web-public.aat.platform.hmcts.net",
    "CCD_URL=http://ccd-data-store-api-aat.service.core-compute-aat.internal"
})
public abstract class CftRepositoryBaseTest {
    //Marker class

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:11"))
        .withDatabaseName("cft_task_db")
        .withUsername("postgres")
        .withPassword("pass");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> String.format(
                "jdbc:postgresql://localhost:%d/%s",
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName()
            )
        );
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }
}
