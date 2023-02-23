package uk.gov.hmcts.reform.wataskmanagementapi.domain;

import io.restassured.http.Headers;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class TestAuthenticationCredentials {

    private final TestAccount account;
    private final Headers headers;

    public TestAuthenticationCredentials(TestAccount account, Headers headers) {
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(headers, "headers cannot be null");
        this.account = account;
        this.headers = headers;
    }

    public Headers getHeaders() {
        return headers;
    }

    public TestAccount getAccount() {
        return account;
    }
}
