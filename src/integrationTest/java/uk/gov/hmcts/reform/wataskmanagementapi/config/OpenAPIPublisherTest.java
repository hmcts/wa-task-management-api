package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class OpenAPIPublisherTest extends SpringBootIntegrationBaseTest {

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
