package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateTaskRequestTaskPermissionsInner
 */
@JsonTypeName("CreateTaskRequest_task_permissions_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:27:37.331356Z[Europe/London]")public class CreateTaskRequestTaskPermissionsInner {

  private String roleName;

  private String roleCategory;

  
  private List<uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes> permissions = new ArrayList<>();

  
  private List<String> authorisations;

  private Integer assignmentPriority;

  private Boolean autoAssignable;

  public CreateTaskRequestTaskPermissionsInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateTaskRequestTaskPermissionsInner(String roleName, String roleCategory, List<uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes> permissions) {
    this.roleName = roleName;
    this.roleCategory = roleCategory;
    this.permissions = permissions;
  }

  public CreateTaskRequestTaskPermissionsInner roleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  /**
   * Role name.
   * @return roleName
  */
  @NotNull
  @Schema(name = "role_name", description = "Role name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role_name")
  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public CreateTaskRequestTaskPermissionsInner roleCategory(String roleCategory) {
    this.roleCategory = roleCategory;
    return this;
  }

  /**
   * Role category.
   * @return roleCategory
  */
  @NotNull
  @Schema(name = "role_category", description = "Role category.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role_category")
  public String getRoleCategory() {
    return roleCategory;
  }

  public void setRoleCategory(String roleCategory) {
    this.roleCategory = roleCategory;
  }

  public CreateTaskRequestTaskPermissionsInner permissions(List<uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes> permissions) {
    this.permissions = permissions;
    return this;
  }

  public CreateTaskRequestTaskPermissionsInner addPermissionsItem(uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes permissionsItem) {
    if (this.permissions == null) {
      this.permissions = new ArrayList<>();
    }
    this.permissions.add(permissionsItem);
    return this;
  }

  /**
   * Get permissions
   * @return permissions
  */
  @NotNull
  @Schema(name = "permissions", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("permissions")
  public List<uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes> permissions) {
    this.permissions = permissions;
  }

  public CreateTaskRequestTaskPermissionsInner authorisations(List<String> authorisations) {
    this.authorisations = authorisations;
    return this;
  }

  public CreateTaskRequestTaskPermissionsInner addAuthorisationsItem(String authorisationsItem) {
    if (this.authorisations == null) {
      this.authorisations = new ArrayList<>();
    }
    this.authorisations.add(authorisationsItem);
    return this;
  }

  /**
   * Authorisations for the role.
   * @return authorisations
  */
  
  @Schema(name = "authorisations", description = "Authorisations for the role.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorisations")
  public List<String> getAuthorisations() {
    return authorisations;
  }

  public void setAuthorisations(List<String> authorisations) {
    this.authorisations = authorisations;
  }

  public CreateTaskRequestTaskPermissionsInner assignmentPriority(Integer assignmentPriority) {
    this.assignmentPriority = assignmentPriority;
    return this;
  }

  /**
   * Assignment priority.
   * @return assignmentPriority
  */
  
  @Schema(name = "assignment_priority", description = "Assignment priority.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("assignment_priority")
  public Integer getAssignmentPriority() {
    return assignmentPriority;
  }

  public void setAssignmentPriority(Integer assignmentPriority) {
    this.assignmentPriority = assignmentPriority;
  }

  public CreateTaskRequestTaskPermissionsInner autoAssignable(Boolean autoAssignable) {
    this.autoAssignable = autoAssignable;
    return this;
  }

  /**
   * Whether the role is auto-assignable.
   * @return autoAssignable
  */
  
  @Schema(name = "auto_assignable", description = "Whether the role is auto-assignable.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("auto_assignable")
  public Boolean getAutoAssignable() {
    return autoAssignable;
  }

  public void setAutoAssignable(Boolean autoAssignable) {
    this.autoAssignable = autoAssignable;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateTaskRequestTaskPermissionsInner createTaskRequestTaskPermissionsInner = (CreateTaskRequestTaskPermissionsInner) o;
    return Objects.equals(this.roleName, createTaskRequestTaskPermissionsInner.roleName) &&
        Objects.equals(this.roleCategory, createTaskRequestTaskPermissionsInner.roleCategory) &&
        Objects.equals(this.permissions, createTaskRequestTaskPermissionsInner.permissions) &&
        Objects.equals(this.authorisations, createTaskRequestTaskPermissionsInner.authorisations) &&
        Objects.equals(this.assignmentPriority, createTaskRequestTaskPermissionsInner.assignmentPriority) &&
        Objects.equals(this.autoAssignable, createTaskRequestTaskPermissionsInner.autoAssignable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleName, roleCategory, permissions, authorisations, assignmentPriority, autoAssignable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTaskRequestTaskPermissionsInner {\n");
    sb.append("    roleName: ").append(toIndentedString(roleName)).append("\n");
    sb.append("    roleCategory: ").append(toIndentedString(roleCategory)).append("\n");
    sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
    sb.append("    authorisations: ").append(toIndentedString(authorisations)).append("\n");
    sb.append("    assignmentPriority: ").append(toIndentedString(assignmentPriority)).append("\n");
    sb.append("    autoAssignable: ").append(toIndentedString(autoAssignable)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

