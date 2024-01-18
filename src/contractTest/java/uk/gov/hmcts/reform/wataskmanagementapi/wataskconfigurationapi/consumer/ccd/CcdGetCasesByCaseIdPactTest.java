package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.consumer.ccd;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdDataService;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@PactTestFor(providerName = "ccdDataStoreAPI_Cases", port = "8891")
@ContextConfiguration(classes = {CcdConsumerApplication.class})
public class CcdGetCasesByCaseIdPactTest extends SpringBootContractBaseTest {

    private static final String TEST_CASE_ID = "1607103938250138";
    private static final String CCD_CASE_URL = "/cases/" + TEST_CASE_ID;

    @Autowired
    CcdDataServiceApi ccdDataServiceApi;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @MockBean
    IdamTokenGenerator systemTokenGenerator;

    private CcdDataService ccdDataService;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(systemTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        ccdDataService = new CcdDataService(ccdDataServiceApi, authTokenGenerator, systemTokenGenerator);
    }

    @Pact(provider = "ccd_data_store_get_case_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeCcdGetCasesByCaseId(PactDslWithProvider builder) {

        Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");

        return builder
            .given("a case exists")
            .uponReceiving("Provider receives a GET /cases/{caseId} request from a WA API")
            .path(CCD_CASE_URL)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseHeaders)
            .body(createCasesResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeCcdGetCasesByCaseId", pactVersion = PactSpecVersion.V3)
    public void verifyGetCaseById() {

        CaseDetails caseDetails = ccdDataService.getCaseData(TEST_CASE_ID);

        assertThat(caseDetails.getSecurityClassification(), is("PRIVATE"));
        assertThat(caseDetails.getJurisdiction(), is("IA"));
        assertThat(caseDetails.getCaseType(), is("Asylum"));
    }

    private PactDslJsonBody createCasesResponse() {
        return new PactDslJsonBody()
            .stringType("id", "1593694526480034")
            .stringValue("jurisdiction", "IA")
            .stringValue("case_type", "Asylum")
            .stringValue("security_classification", "PRIVATE");
    }

    private CaseDetails read(String caseData) {
        try {
            return new ObjectMapper().readValue(caseData, CaseDetails.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                String.format("Cannot parse result from CCD for %s", CcdGetCasesByCaseIdPactTest.TEST_CASE_ID),
                ex
            );
        }
    }
}
