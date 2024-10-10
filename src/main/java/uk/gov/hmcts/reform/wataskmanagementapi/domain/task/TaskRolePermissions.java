package uk.gov.hmcts.reform.wataskmanagementapi.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@EqualsAndHashCode
public class TaskRolePermissions {

    @Schema(
        name = "role_category",
        requiredMode = REQUIRED,
        description = "A value describing the role category"
    )
    private final String roleCategory;

    @Schema(
        name = "role_name",
        requiredMode = REQUIRED,
        description = "Name of the role"
    )
    private final String roleName;

    @Schema(
        requiredMode = REQUIRED,
        description = "Task role permission types"
    )
    private final List<PermissionTypes> permissions;

    @Schema(
        requiredMode = REQUIRED,
        description = "Authorisations"
    )
    private final List<String> authorisations;

    @JsonCreator
    public TaskRolePermissions(String roleCategory, String roleName, List<PermissionTypes> permissions,
                               List<String> authorisations) {
        this.roleCategory = roleCategory;
        this.roleName = roleName;
        this.permissions = permissions;
        this.authorisations = authorisations;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public String getRoleName() {
        return roleName;
    }

    public List<PermissionTypes> getPermissions() {
        return permissions;
    }

    public List<String> getAuthorisations() {
        return authorisations;
    }
}
