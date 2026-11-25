package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode
@ToString
public class Token {

    private String accessToken;
    private String scope;

    private Token() {
        //No-op constructor for deserialization
    }

    public Token(String accessToken, String scope) {
        this.accessToken = accessToken;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getScope() {
        return scope;
    }

}
