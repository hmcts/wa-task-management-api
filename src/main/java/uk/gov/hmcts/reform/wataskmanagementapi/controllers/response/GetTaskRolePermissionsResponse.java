package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetTaskRolePermissionsResponse {

    @JsonProperty("roles")
    private final List<TaskRolePermissions> permissionsList;

    public GetTaskRolePermissionsResponse(List<TaskRolePermissions> permissionsList) {
        this.permissionsList = permissionsList;
    }

    public List<TaskRolePermissions> getPermissionsList() {
        return permissionsList;
    }
}
