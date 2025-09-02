package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util.CcdConsumerTestBase;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util.PactDslBuilderForCaseDetailsList.buildSubmitForCaseWorkedPactDsl;

public class SubmitForCaseWorkerConsumerTest extends CcdConsumerTestBase {

    @Override
    public Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent)
        throws JSONException {

        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithCaseData(caseDataContent);
        caseDataContentMap.put(EVENT_ID, SUBMIT_APPEAL);
        return caseDataContentMap;
    }

    @Pact(provider = "ccdDataStoreAPI_WorkAllocation", consumer = "wa_task_management_api")
    public RequestResponsePact submitCaseWorkerDetails(PactDslWithProvider builder) throws Exception {
        return builder
            .given("A Submit for a Caseworker is requested",
                setUpStateMapForProviderWithoutCaseData())
            .uponReceiving("A Submit For a Caseworker")
            .path(buildPath())
            .query("ignore-warning=true")
            .method("POST")
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .matchHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonString(createCaseDataContent(SUBMIT_APPEAL, caseDetailsMap)))
            .willRespondWith()
            .status(HttpStatus.SC_CREATED)
            .body(buildSubmitForCaseWorkedPactDsl(CASE_ID))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "submitCaseWorkerDetails", pactVersion = PactSpecVersion.V3)
    public void submitForCaseWorker() {

        caseDataContent = createCaseDataContent(SUBMIT_APPEAL, caseDetailsMap);
        CaseDetails caseDetailsResponse = coreCaseDataApi.submitForCaseworker(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            USER_ID,
            "IA",
            "Asylum",
            true,
            caseDataContent);

        assertEquals(caseDetailsResponse.getCaseTypeId(), "Asylum");
    }

    private String buildPath() {
        return new StringBuilder()
            .append("/caseworkers/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append("IA")
            .append("/case-types/")
            .append("Asylum")
            .append("/cases")
            .toString();
    }
}
