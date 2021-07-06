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

public class CcdSubmitEventForCaseworkerPactTest extends SpringBootContractBaseTest {


    private static final String CCD_SUBMIT_EVENT_FOR_CASEWORKER = "/caseworkers/00/jurisdictions/ia/"
                                                                  + "case-types/asylum/cases/0000/events";

    @Pact(provider = "ccd_data_store_submit_event", consumer = "wa_task_management_api")
    public RequestResponsePact executeSubmitEventForCaseworker(PactDslWithProvider builder) {

        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/json");

        return builder
            .given("Submit event creation as Case worker")
            .uponReceiving("Complete the event creation process request from a WA API")
            .path(CCD_SUBMIT_EVENT_FOR_CASEWORKER)
            .method(HttpMethod.POST.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseHeaders)
            .body(eventsCasesResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeSubmitEventForCaseworker")
    public void should_post_to_submit_event_for_caseworker_endpoint_with_200_response(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .contentType(ContentType.URLENC)
                .log().all(true)
                .post(mockServer.getUrl() + CCD_SUBMIT_EVENT_FOR_CASEWORKER)
                .then()
                .extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
        assertThat(response.getString("case_reference")).isEqualTo("string");


    }

    private PactDslJsonBody eventsCasesResponse() {
        return new PactDslJsonBody()
            .stringType("case_reference", "string")
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
            .stringValue("draft_id", "string")
            .object("event")
            .numberValue("delete_draft_response_status_code", 0)
            .stringValue("description", "string")
            .stringValue("id", "string")
            .stringValue("summary", "string")
            .close()
            .object("event_data")
            .object("additionalProp1", new PactDslJsonBody())
            .object("additionalProp2", new PactDslJsonBody())
            .object("additionalProp3", new PactDslJsonBody())
            .close()
            .asBody()
            .stringValue("event_token", "PRIVATE")
            .booleanValue("ignore_warning", true)
            .stringValue("on_behalf_of_token", "string")
            .stringValue("security_classification", "string");

    }
}

