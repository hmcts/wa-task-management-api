package uk.gov.hmcts.reform.wataskmanagementapi;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.core.model.annotations.PactFolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactFolder("pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = {"classpath:application.properties"})
public abstract class SpringBootContractBaseTest {

    public static final String PACT_TEST_EMAIL_VALUE = "ia-caseofficer@fake.hmcts.net";
    public static final String PACT_TEST_PASSWORD_VALUE = "London01";
    public static final String PACT_TEST_CLIENT_ID_VALUE = "pact";
    public static final String PACT_TEST_CLIENT_SECRET_VALUE = "pactsecret";
    public static final String PACT_TEST_SCOPES_VALUE = "openid profile roles";
    public static final String PACT_TEST_ROLES_VALUE = "caseworker";
    public static final String AUTHORIZATION_BEARER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdeRre";
    public static final String SERVICE_BEARER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92V";
    public static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    public static final String EXPERIMENTAL = "true";


    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", SERVICE_BEARER_TOKEN);
        headers.add("Experimental", EXPERIMENTAL);
        headers.add("Authorization", AUTHORIZATION_BEARER_TOKEN);
        return headers;
    }
}
