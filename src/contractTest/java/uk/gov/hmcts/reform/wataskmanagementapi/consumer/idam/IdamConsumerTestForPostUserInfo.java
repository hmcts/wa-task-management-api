package uk.gov.hmcts.reform.wataskmanagementapi.consumer.idam;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ContextConfiguration(classes = {IdamConsumerApplication.class})
public class IdamConsumerTestForPostUserInfo extends SpringBootContractBaseTest {

    @Autowired
    IdamWebApi idamApi;

    @Pact(provider = "idamApi_oidc", consumer = "wa_task_management_api")
    public RequestResponsePact generatePactFragmentUserInfo(PactDslWithProvider builder) throws JSONException {

        return builder
            .given("userinfo is requested")
            .uponReceiving("A request for a UserInfo")
            .path("/o/userinfo")
            .method("GET")
            .matchHeader("Authorization", AUTH_TOKEN)
            .matchHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .status(200)
            .body(createUserDetailsResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragmentUserInfo")
    public void verifyIdamUserDetailsRolesPactUserInfo() {
        UserInfo userInfo = idamApi.userInfo(AUTH_TOKEN);
        assertEquals(PACT_TEST_EMAIL_VALUE, userInfo.getEmail(), "User is not Case Officer");
    }

    private PactDslJsonBody createUserDetailsResponse() {
        PactDslJsonArray array = new PactDslJsonArray().stringValue("caseofficer-ia");

        return new PactDslJsonBody()
            .stringType("uid", "1111-2222-3333-4567")
            .stringValue("sub", "ia-caseofficer@fake.hmcts.net")
            .stringValue("givenName", "Case")
            .stringValue("familyName", "Officer")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("caseworker-ia-legalrep-solicitor"), 1)
            .stringType("IDAM_ADMIN_USER");
    }
}
