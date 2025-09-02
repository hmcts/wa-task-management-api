package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util.CcdConsumerTestBase;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd.util.PactDslBuilderForCaseDetailsList.buildStartEventForCaseWorkerPactDsl;

public class StartEventForCaseWorkerConsumerTest extends CcdConsumerTestBase {

    public Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent)
        throws JSONException {
        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithCaseData(caseDataContent);
        caseDataContentMap.put(EVENT_ID, START_APPEAL);
        return caseDataContentMap;
    }

    @Pact(provider = "ccdDataStoreAPI_WorkAllocation", consumer = "wa_task_management_api")
    public RequestResponsePact startEventForCaseWorker(PactDslWithProvider builder) throws JSONException {
        return builder
            .given("A Start Event for a Caseworker is  requested",
                setUpStateMapForProviderWithCaseData(caseDataContent)
            )
            .uponReceiving("A Start Event for a Caseworker")
            .path(buildPath())
            .method("GET")
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .matchHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildStartEventForCaseWorkerPactDsl(START_APPEAL))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "startEventForCaseWorker", pactVersion = PactSpecVersion.V3)
    public void verifyStartEventForCaseworker() {

        final StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            AUTH_TOKEN,
            SERVICE_AUTH_TOKEN,
            USER_ID,
            "IA",
            "Asylum",
            CASE_ID.toString(),
            START_APPEAL);

        assertThat(startEventResponse.getEventId(), is(START_APPEAL));
        assertNotNull(startEventResponse.getToken());
        assertCaseDetails(startEventResponse.getCaseDetails());
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
            .append("/event-triggers/")
            .append(START_APPEAL)
            .append("/token")
            .toString();
    }

    private void assertCaseDetails(final CaseDetails caseDetails) {
        assertNotNull(caseDetails);

        Map<String, Object> caseDataMap = caseDetails.getData();

        assertThat(caseDataMap.get("appellantTitle"), is("Mr"));
        assertThat(caseDataMap.get("appellantGivenNames"), is("Bob"));
        assertThat(caseDataMap.get("appellantFamilyName"), is("Smith"));
        assertThat(caseDataMap.get("appellantDateOfBirth"), is("1990-12-07"));
        assertThat(caseDataMap.get("homeOfficeReferenceNumber"), is("000123456"));
        assertThat(caseDataMap.get("homeOfficeDecisionDate"), is("2019-08-01"));
        assertThat(caseDataMap.get("appealType"), is("protection"));
        assertThat(caseDataMap.get("submissionOutOfTime"), is("Yes"));
        assertThat(caseDataMap.get("applicationOutOfTimeExplanation"), is("test case"));

        //caseManagementLocation
        @SuppressWarnings("unchecked")
        Map<String, String> caseManagementLocation = (Map<String, String>) caseDataMap.get("caseManagementLocation");
        assertThat(caseManagementLocation.get("region"), is("1"));
        assertThat(caseManagementLocation.get("baseLocation"), is("765324"));

        assertThat(caseDataMap.get("staffLocation"), is("Taylor House"));

    }
}
