package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import lombok.Builder;
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
public class Assignment {

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

    private Assignment() {
        //Hidden constructor
    }

    @JsonCreator
    public Assignment(@JsonProperty("actorIdType") ActorIdType actorIdType,
                      @JsonProperty("actorId") String actorId,
                      @JsonProperty("roleType") RoleType roleType,
                      @JsonProperty("roleName") String roleName,
                      @JsonProperty("classification") Classification classification,
                      @JsonProperty("grantType") GrantType grantType,
                      @JsonProperty("roleCategory") RoleCategory roleCategory,
                      @JsonProperty("readOnly") boolean readOnly,
                      @JsonProperty("attributes") Map<String, String> attributes
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

    @JsonCreator
    public Assignment(@JsonProperty("id") String id,
                      @JsonProperty("actorIdType") ActorIdType actorIdType,
                      @JsonProperty("actorId") String actorId,
                      @JsonProperty("roleType") RoleType roleType,
                      @JsonProperty("roleName") String roleName,
                      @JsonProperty("classification") Classification classification,
                      @JsonProperty("grantType") GrantType grantType,
                      @JsonProperty("roleCategory") RoleCategory roleCategory,
                      @JsonProperty("readOnly") boolean readOnly,
                      @JsonProperty("beginTime") LocalDateTime beginTime,
                      @JsonProperty("endTime") LocalDateTime endTime,
                      @JsonProperty("created") LocalDateTime created,
                      @JsonProperty("attributes") Map<String, String> attributes,
                      @JsonProperty("authorisations") List<String> authorisations) {
        this.id = id;
        this.actorIdType = actorIdType;
        this.actorId = actorId;
        this.roleType = roleType;
        this.roleName = roleName;
        this.classification = classification;
        this.grantType = grantType;
        this.roleCategory = roleCategory;
        this.readOnly = readOnly;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.created = created;
        this.attributes = attributes;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Assignment that = (Assignment) object;
        return readOnly == that.readOnly
               && Objects.equal(id, that.id)
               && actorIdType == that.actorIdType
               && Objects.equal(actorId, that.actorId)
               && roleType == that.roleType
               && Objects.equal(roleName, that.roleName)
               && classification == that.classification
               && grantType == that.grantType
               && roleCategory == that.roleCategory
               && Objects.equal(beginTime, that.beginTime)
               && Objects.equal(endTime, that.endTime)
               && Objects.equal(created, that.created)
               && Objects.equal(attributes, that.attributes)
               && Objects.equal(authorisations, that.authorisations);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
            id,
            actorIdType,
            actorId,
            roleType,
            roleName,
            classification,
            grantType,
            roleCategory,
            readOnly,
            beginTime,
            endTime,
            created,
            attributes,
            authorisations
        );
    }

    @Override
    public String toString() {
        return "Assignment{"
               + "id=" + id
               + ", actorIdType=" + actorIdType
               + ", actorId='" + actorId
               + ", roleType=" + roleType
               + ", roleName='" + roleName
               + ", classification=" + classification
               + ", grantType=" + grantType
               + ", roleCategory=" + roleCategory
               + ", readOnly=" + readOnly
               + ", beginTime=" + beginTime
               + ", endTime=" + endTime
               + ", created=" + created
               + ", attributes=" + attributes
               + ", authorisations=" + authorisations
               + '}';
    }
}
