package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.ExcessiveParameterList")
@Builder
@EqualsAndHashCode
@ToString
public class RoleAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private ActorIdType actorIdType;
    private String actorId;
    private RoleType roleType;
    private String roleName;
    private Classification classification;
    private GrantType grantType;
    private RoleCategory roleCategory;
    private boolean readOnly;
    private OffsetDateTime beginTime;
    private OffsetDateTime endTime;
    private OffsetDateTime created;
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
                          OffsetDateTime beginTime,
                          OffsetDateTime endTime,
                          OffsetDateTime created,
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

    public Optional<String> getAttributeValue(RoleAttributeDefinition key) {
        if (attributes != null && key != null) {
            return Optional.ofNullable(this.attributes.get(key.value()));
        }
        return Optional.empty();
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

    public OffsetDateTime getBeginTime() {
        return beginTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<String> getAuthorisations() {
        return authorisations;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("actorIdType")
    public void setActorIdType(ActorIdType actorIdType) {
        this.actorIdType = actorIdType;
    }

    @JsonProperty("actorId")
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    @JsonProperty("roleType")
    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    @JsonProperty("roleName")
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    @JsonProperty("grantType")
    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

    @JsonProperty("roleCategory")
    public void setRoleCategory(RoleCategory roleCategory) {
        this.roleCategory = roleCategory;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setBeginTime(OffsetDateTime beginTime) {
        this.beginTime = beginTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setAuthorisations(List<String> authorisations) {
        this.authorisations = authorisations;
    }

}
