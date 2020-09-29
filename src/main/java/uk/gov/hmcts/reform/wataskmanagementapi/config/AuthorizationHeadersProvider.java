package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.ServiceTokenGeneratorConfiguration.SERVICE_AUTHORIZATION;


@Service
public class AuthorizationHeadersProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    public Headers getAuthorizationHeaders() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION, user -> serviceAuthTokenGenerator.generate());

        return new Headers(
            new Header(SERVICE_AUTHORIZATION, serviceToken)
        );
    }

}
