package uk.gov.hmcts.reform.wataskmanagementapi;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.core.model.annotations.PactFolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

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
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
<<<<<<< HEAD
    public static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";
<<<<<<< HEAD
=======
=======
>>>>>>> 6e477b6c078906bb1812d0fd38917be42e32989d
>>>>>>> 7275f41d0b8e5ccdecd242b16d1a8097fec3e1e6
    public static final String EXPERIMENTAL = "true";


    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
<<<<<<< HEAD
        headers.add(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN);
        headers.add(AUTHORIZATION, AUTH_TOKEN);
        headers.add("Experimental", EXPERIMENTAL);
<<<<<<< HEAD
=======
=======
        headers.add("ServiceAuthorization", SERVICE_BEARER_TOKEN);
        headers.add("Experimental", EXPERIMENTAL);
        headers.add("Authorization", AUTHORIZATION_BEARER_TOKEN);
>>>>>>> 6e477b6c078906bb1812d0fd38917be42e32989d
>>>>>>> 7275f41d0b8e5ccdecd242b16d1a8097fec3e1e6
        return headers;
    }
}
