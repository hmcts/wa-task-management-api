package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "IDAM_URL=https://idam-api.aat.platform.hmcts.net",
    "OPEN_ID_IDAM_URL=https://idam-web-public.aat.platform.hmcts.net",
    "CCD_URL=http://ccd-data-store-api-aat.service.core-compute-aat.internal"
})
public class SpringBootIntegrationBaseTest {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    protected String asJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

}
