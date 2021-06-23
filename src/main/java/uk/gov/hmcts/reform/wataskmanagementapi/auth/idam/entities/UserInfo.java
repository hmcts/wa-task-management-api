package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder
@EqualsAndHashCode
@ToString
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
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        UserInfo userInfo = (UserInfo) object;
        return Objects.equals(email, userInfo.email)
               && Objects.equals(uid, userInfo.uid)
               && Objects.equals(roles, userInfo.roles)
               && Objects.equals(name, userInfo.name)
               && Objects.equals(givenName, userInfo.givenName)
               && Objects.equals(familyName, userInfo.familyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, uid, roles, name, givenName, familyName);
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
