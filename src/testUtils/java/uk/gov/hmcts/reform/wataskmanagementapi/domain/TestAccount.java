package uk.gov.hmcts.reform.wataskmanagementapi.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class TestAccount {

    private final String username;
    private final String password;

    public TestAccount(String username, String password) {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
