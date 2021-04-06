package uk.gov.hmcts.reform.wataskmanagementapi.consumer.ccd;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CcdCasePactTest extends SpringBootContractBaseTest {

    private static final String TEST_CASE_ID = "1607103938250138";
    private static final String CCD_CASE_URL = "/cases/" + TEST_CASE_ID;

    @Pact(provider = "ccd_data_store", consumer = "wa_task_configuration_api")
    public RequestResponsePact ccdGetCasesId(PactDslWithProvider builder) {

        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/json");

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
    @PactTestFor(pactMethod = "ccdGetCasesId")
    public void should_post_to_token_endpoint_and_receive_access_token_with_200_response(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .contentType(ContentType.URLENC)
                .log().all(true)
                .get(mockServer.getUrl() + CCD_CASE_URL)
                .then()
                .extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
        assertThat(response.getString("callback_response_status")).isNotBlank();
        assertThat(response.getString("callback_response_status_code")).isEqualTo("0");
        assertThat(response.getString("case_type")).isNotBlank();

    }

    private PactDslJsonBody createCasesResponse() {

        return new PactDslJsonBody()
            .object("after_submit_callback_response")
                .stringType("confirmation_body", "string")
                .stringType("confirmation_header", "string")
            .close()
            .asBody()
            .stringValue("callback_response_status", "string")
            .numberValue("callback_response_status_code", 0)
            .stringValue("case_type", "string")
            .stringValue("created_on", "2021-03-24T09:08:32.869Z")
            .close()
            .object("data")
                .object("additionalProp1", new PactDslJsonBody())
                .object("additionalProp2", new PactDslJsonBody())
                .object("additionalProp3", new PactDslJsonBody())
            .close()
            .object("data_classification")
            .object("additionalProp1", new PactDslJsonBody())
            .object("additionalProp2", new PactDslJsonBody())
            .object("additionalProp3", new PactDslJsonBody())
            .close()
            .asBody()
                 .stringValue("delete_draft_response_status", "string")
                 .numberValue("delete_draft_response_status_code", 0)
                 .stringValue("id", "string")
                 .stringValue("jurisdiction", "string")
                 .stringValue("last_modified_on", "2021-03-24T09:08:32.869Z")
                 .stringValue("last_state_modified_on", "2021-03-24T09:08:32.869Z")
            .close()
            .object("links")
                .booleanValue("empty", true)
            .close()
            .asBody()
                    .stringValue("security_classification", "PRIVATE")
                    .stringValue("state", "string");

    }
}

