package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TransactionHelper;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "IDAM_URL=https://idam-api.aat.platform.hmcts.net",
    "OPEN_ID_IDAM_URL=https://idam-web-public.aat.platform.hmcts.net",
    "CCD_URL=http://ccd-data-store-api-aat.service.core-compute-aat.internal"
})
@TestInstance(PER_CLASS)
public abstract class SpringBootIntegrationBaseTest {
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    protected TransactionHelper transactionHelper;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected Jackson2ObjectMapperBuilder mapperBuilder;

    protected ObjectMapper objectMapper;

    protected String asJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    protected String asJsonStringHandled(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error writing value to string");
        }
    }

    @BeforeAll
    public void initObjectMapper() {
        //Initialize mapper as defined in JacksonConfiguration.class
        objectMapper = mapperBuilder.build();
    }
}
