package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles("integration")
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "IDAM_API_URL=https://idam-api.aat.platform.hmcts.net",
    "OPEN_ID_IDAM_URL=https://idam-web-public.aat.platform.hmcts.net",
    "CCD_URL=http://ccd-data-store-api-aat.service.core-compute-aat.internal"
})
@AutoConfigureMockMvc(addFilters = false)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SpringBootIntegrationBaseTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

}
