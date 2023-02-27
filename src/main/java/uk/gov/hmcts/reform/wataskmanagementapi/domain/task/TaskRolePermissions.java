package uk.gov.hmcts.reform.wataskmanagementapi.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@EqualsAndHashCode
public class TaskRolePermissions {

    private final String roleCategory;

    private final String roleName;

    private final List<PermissionTypes> permissions;

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
