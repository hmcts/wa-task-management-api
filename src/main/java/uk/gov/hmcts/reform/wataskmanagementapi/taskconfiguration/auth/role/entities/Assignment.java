package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums.RoleType;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class Assignment {
    protected final ActorIdType actorIdType;
    protected final String actorId;
    protected final RoleType roleType;
    protected final String roleName;
    protected final RoleCategory roleCategory;
    protected final Classification classification;
    protected final GrantType grantType;
    protected final Boolean readOnly;
    protected final Map<String, String> attributes;


    public Assignment(ActorIdType actorIdType,
                      String actorId,
                      RoleType roleType,
                      String roleName,
                      RoleCategory roleCategory,
                      Classification classification,
                      GrantType grantType,
                      Boolean readOnly,
                      Map<String, String> attributes) {
        this.actorIdType = actorIdType;
        this.actorId = actorId;
        this.roleType = roleType;
        this.roleName = roleName;
        this.roleCategory = roleCategory;
        this.classification = classification;
        this.grantType = grantType;
        this.readOnly = readOnly;
        this.attributes = attributes;
    }

    public ActorIdType getActorIdType() {
        return actorIdType;
    }

    public String getActorId() {
        return actorId;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public String getRoleName() {
        return roleName;
    }

    public RoleCategory getRoleCategory() {
        return roleCategory;
    }

    public Classification getClassification() {
        return classification;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
