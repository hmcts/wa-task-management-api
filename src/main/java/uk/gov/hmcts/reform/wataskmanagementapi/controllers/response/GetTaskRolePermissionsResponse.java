package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetTaskRolePermissionsResponse {

    @JsonProperty("roles")
    @Schema(
        name = "permissions_list",
        requiredMode = REQUIRED,
        description = "Task role permission list"
    )
    private final List<TaskRolePermissions> permissionsList;

    public GetTaskRolePermissionsResponse(List<TaskRolePermissions> permissionsList) {
        this.permissionsList = permissionsList;
    }

    public List<TaskRolePermissions> getPermissionsList() {
        return permissionsList;
    }
}
