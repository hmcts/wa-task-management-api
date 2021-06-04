package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
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
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util.PactDslBuilderForCaseDetailsList.buildSubmitForCaseWorkedPactDsl;

public class SubmitEventForCaseWorkerConsumerTest extends CcdConsumerTestBase {

    @Override
    protected Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent) throws JSONException {
        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithCaseData(caseDataContent);
        caseDataContentMap.put(EVENT_ID, SUBMIT_APPEAL);
        return caseDataContentMap;
    }

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "wa_task_management_api")
    public RequestResponsePact submitEventForCaseWorker(PactDslWithProvider builder) throws Exception {
        return builder
            .given("A Submit Event for a Caseworker is requested", setUpStateMapForProviderWithCaseData(caseDataContent))
            .uponReceiving("A Submit Event for a Caseworker")
            .path(buildPath())
            .query("ignore-warning=true")
            .method("POST")
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .matchHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonString(createCaseDataContent(SUBMIT_APPEAL, caseDetailsMap)))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .status(201)
            .body(buildSubmitForCaseWorkedPactDsl(CASE_ID))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "submitEventForCaseWorker")
    public void verifySubmitEventForCaseworker() throws Exception {

        final CaseDetails caseDetails = coreCaseDataApi.submitEventForCaseWorker(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            USER_ID,
            "IA",
            "Asylum",
            CASE_ID.toString(),
            true,
            caseDataContent);

        assertThat(caseDetails.getId(), is(CASE_ID));
        assertThat(caseDetails.getJurisdiction(), is("DIVORCE"));
        assertCaseDetails(caseDetails);
    }

    private void assertCaseDetails(final CaseDetails caseDetails) {
        assertNotNull(caseDetails);

        Map<String, Object> caseDataMap = caseDetails.getData();

        assertThat(caseDataMap.get("appellantTitle"), is("Mr"));
        assertThat(caseDataMap.get("appellantGivenNames"), is("Bob"));
        assertThat(caseDataMap.get("appellantGivenNames"), is("Smith"));
        assertThat(caseDataMap.get("appellantDateOfBirth"), is("1990-12-07"));
        assertThat(caseDataMap.get("homeOfficeReferenceNumber"), is("000123456"));
        assertThat(caseDataMap.get("homeOfficeDecisionDate"), is("2019-08-01"));
        assertThat(caseDataMap.get("appealType"), is("protection"));
        assertThat(caseDataMap.get("submissionOutOfTime"), is("Yes"));
        assertThat(caseDataMap.get("applicationOutOfTimeExplanation"), is("test case"));

        //caseManagementLocation
        Map<String, String> caseManagementLocation = (Map<String, String>) caseDataMap.get("caseManagementLocation");
        assertThat(caseManagementLocation.get("region"), is("1"));
        assertThat(caseManagementLocation.get("baseLocation"), is("765324"));

        assertThat(caseDataMap.get("staffLocation"), is("Taylor House"));

    }

    private String buildPath() {
        return new StringBuilder()
            .append("/caseworkers/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append("IA")
            .append("/case-types/")
            .append("Asylum")
            .append("/cases/")
            .append(CASE_ID)
            .append("/events")
            .toString();
    }
}
