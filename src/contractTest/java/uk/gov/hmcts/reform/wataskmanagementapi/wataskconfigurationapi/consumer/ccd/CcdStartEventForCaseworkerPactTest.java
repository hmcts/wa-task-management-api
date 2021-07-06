package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.consumer.ccd;

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

public class CcdStartEventForCaseworkerPactTest extends SpringBootContractBaseTest {

    private static final String CCD_START_FOR_CASEWORKER = "/caseworkers/0000/jurisdictions/ia/case-types/"
                                                           + "asylum/event-triggers/tester/token";

    @Pact(provider = "ccd_data_store_start_event", consumer = "wa_task_management_api")
    public RequestResponsePact executeStartEventForCaseworker(PactDslWithProvider builder) {

        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/json");

        return builder
            .given("Start event creation as Case worker")
            .uponReceiving("Start the event creation process for an existing case from a WA API")
            .path(CCD_START_FOR_CASEWORKER)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseHeaders)
            .body(startForCaseworkerResponse())
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "executeStartEventForCaseworker")
    public void should_get_start_for_caseworker_to_token_endpoint(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .contentType(ContentType.URLENC)
                .log().all(true)
                .get(mockServer.getUrl() + CCD_START_FOR_CASEWORKER)
                .then()
                .extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
    }

    private PactDslJsonBody startForCaseworkerResponse() {

        return new PactDslJsonBody()
            .object("case_details")
            .object("after_submit_callback_response")
            .stringValue("confirmation_body", "string")
            .stringValue("confirmation_header", "string")
            .close()
            .asBody()
            .stringType("callback_response_status", "string")
            .numberValue("callback_response_status_code", 0)
            .object("case_data")
            .object("additionalProp1", new PactDslJsonBody())
            .object("additionalProp2", new PactDslJsonBody())
            .object("additionalProp3", new PactDslJsonBody())
            .close()
            .asBody()
            .stringValue("case_type_id", "string")
            .stringValue("created_date", "2021-04-07T08:51:52.452Z")
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
            .stringValue("security_classification", "PRIVATE")
            .object("security_classifications")
            .object("additionalProp1", new PactDslJsonBody())
            .object("additionalProp2", new PactDslJsonBody())
            .object("additionalProp3", new PactDslJsonBody())
            .close()
            .asBody()
            .stringValue("state", "string")
            .object("supplementary_data")
            .object("additionalProp1", new PactDslJsonBody())
            .object("additionalProp2", new PactDslJsonBody())
            .object("additionalProp3", new PactDslJsonBody())
            .close()
            .asBody()
            .numberValue("version", 0)
            .close()
            .asBody()
            .stringValue("event_id", "string")
            .stringValue("token", "string");


    }

}

