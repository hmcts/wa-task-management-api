package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.ExcessiveParameterList")
@Builder
@EqualsAndHashCode
@ToString
@Setter
public class RoleAssignment {

    private String id;
    private ActorIdType actorIdType;
    private String actorId;
    private RoleType roleType;
    private String roleName;
    private Classification classification;
    private GrantType grantType;
    private RoleCategory roleCategory;
    private boolean readOnly;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime created;
    private Map<String, String> attributes;
    private List<String> authorisations;

    public RoleAssignment() {
        //Default constructor
    }

    public RoleAssignment(ActorIdType actorIdType,
                          String actorId,
                          RoleType roleType,
                          String roleName,
                          Classification classification,
                          GrantType grantType,
                          RoleCategory roleCategory,
                          boolean readOnly,
                          Map<String, String> attributes
    ) {
        this.actorIdType = actorIdType;
        this.actorId = actorId;
        this.roleType = roleType;
        this.roleName = roleName;
        this.classification = classification;
        this.grantType = grantType;
        this.roleCategory = roleCategory;
        this.readOnly = readOnly;
        this.attributes = attributes;
    }

    public RoleAssignment(String id,
                          ActorIdType actorIdType,
                          String actorId,
                          RoleType roleType,
                          String roleName,
                          Classification classification,
                          GrantType grantType,
                          RoleCategory roleCategory,
                          boolean readOnly,
                          LocalDateTime beginTime,
                          LocalDateTime endTime,
                          LocalDateTime created,
                          Map<String, String> attributes,
                          List<String> authorisations) {
        this(actorIdType, actorId, roleType,
            roleName, classification, grantType, roleCategory, readOnly, attributes
        );
        this.id = id;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.created = created;
        this.authorisations = authorisations;
    }

    public String getId() {
        return id;
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

    public Classification getClassification() {
        return classification;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public RoleCategory getRoleCategory() {
        return roleCategory;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<String> getAuthorisations() {
        return authorisations;
    }

}
