package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Transactional//@Testcontainers
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenAPIPublisherTest {

    @Autowired
    protected MockMvc mockMvc;

    private static final String SWAGGER_DOCS_VERSION = "/v3/api-docs";

    @DisplayName("Generate swagger documentation")
    @Test
    void generateDocs() throws Exception {
        byte[] specs = mockMvc.perform(get(SWAGGER_DOCS_VERSION))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs.json"))) {
            outputStream.write(specs);
        }

    }
}
