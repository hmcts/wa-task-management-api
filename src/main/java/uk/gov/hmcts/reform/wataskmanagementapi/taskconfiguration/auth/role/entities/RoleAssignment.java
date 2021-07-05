package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(callSuper = true)
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class RoleAssignment extends Assignment {
    private final String id;
    private final LocalDateTime created;
    private final List<String> authorisations;

    @Builder
    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    private RoleAssignment(String id,
                           ActorIdType actorIdType,
                           String actorId,
                           RoleType roleType,
                           String roleName,
                           RoleCategory roleCategory,
                           Classification classification,
                           GrantType grantType,
                           Boolean readOnly,
                           LocalDateTime created,
                           Map<String, String> attributes,
                           List<String> authorisations) {
        super(actorIdType, actorId, roleType, roleName, roleCategory, classification, grantType, readOnly, attributes);
        this.id = id;
        this.created = created;
        this.authorisations = authorisations;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public List<String> getAuthorisations() {
        return authorisations;
    }

}
