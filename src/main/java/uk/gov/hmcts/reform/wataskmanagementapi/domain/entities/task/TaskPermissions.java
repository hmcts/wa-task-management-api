package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Permissions")
@ToString
@EqualsAndHashCode
public class TaskPermissions {

    @ApiModelProperty(
        notes = "A Union of all permissions held for a task.")
    Set<PermissionTypes> values;

    public TaskPermissions(Set<PermissionTypes> values) {
        this.values = values;
    }

    public Set<PermissionTypes> getValues() {
        return values;
    }
}
