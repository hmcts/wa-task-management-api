package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TaskPermission {

    @NotBlank
    private String roleName;
    private String roleCategory;
    @NotEmpty
    private List<PermissionTypes> permissions;
    private List<String> authorisations = List.of();
    private Integer assignmentPriority = 0;
    private Boolean autoAssignable = false;
}
