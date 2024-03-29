package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

    @JsonProperty("sub")
    private String email;
    private String uid;
    private List<String> roles;
    private String name;
    private String givenName;
    private String familyName;

    public UserInfo() {
        //No-op constructor for deserialization
    }

    public UserInfo(String email, String uid, List<String> roles, String name, String givenName, String familyName) {
        this.email = email;
        this.uid = uid;
        this.roles = roles;
        this.name = name;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getName() {
        return name;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    @Override
    public String toString() {
        return "UserInfo{"
               + "email='" + email + '\''
               + ", uid='" + uid + '\''
               + ", roles=" + roles
               + ", name='" + name + '\''
               + ", givenName='" + givenName + '\''
               + ", familyName='" + familyName + '\''
               + '}';
    }
}
