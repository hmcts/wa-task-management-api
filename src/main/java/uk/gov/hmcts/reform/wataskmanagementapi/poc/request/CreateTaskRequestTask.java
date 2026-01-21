package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequestTaskPermissionsInner;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateTaskRequestTask
 */
@JsonTypeName("CreateTaskRequest_task")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:27:37.331356Z[Europe/London]")public class CreateTaskRequestTask {

  private UUID externalTaskId;

  private String type;

  private String name;

  private String title;

  private String assignee;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime created;

  /**
   * Execution category.
   */
  public enum ExecutionTypeEnum {
    MANUAL("Manual"),
    
    BUILT_IN("Built In"),
    
    CASE_MANAGEMENT_TASK("Case Management Task");

    private String value;

    ExecutionTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ExecutionTypeEnum fromValue(String value) {
      for (ExecutionTypeEnum b : ExecutionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
  private ExecutionTypeEnum executionType;

  private String caseId;

  private String caseTypeId;

  private String caseCategory;

  private String caseName;

  private String jurisdiction;

  private String region;

  private String location;

  private String workType;

  private String roleCategory;

  /**
   * Security classification.
   */
  public enum SecurityClassificationEnum {
    PUBLIC("PUBLIC"),
    
    PRIVATE("PRIVATE"),
    
    RESTRICTED("RESTRICTED");

    private String value;

    SecurityClassificationEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SecurityClassificationEnum fromValue(String value) {
      for (SecurityClassificationEnum b : SecurityClassificationEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
  private SecurityClassificationEnum securityClassification;

  private String description;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dueDateTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime priorityDate;

  private Integer majorPriority;

  private Integer minorPriority;

  private String locationName;

  private String regionName;

  /**
   * Task origin system.
   */
  public enum TaskSystemEnum {
    SELF("SELF"),
    
    CTSC("CTSC");

    private String value;

    TaskSystemEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TaskSystemEnum fromValue(String value) {
      for (TaskSystemEnum b : TaskSystemEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
  private TaskSystemEnum taskSystem;

  
  private Map<String, String> additionalProperties = new HashMap<>();

  
  private List<CreateTaskRequestTaskPermissionsInner> permissions = new ArrayList<>();

  public CreateTaskRequestTask() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateTaskRequestTask(String type, String name, OffsetDateTime created, ExecutionTypeEnum executionType, String caseId, String caseTypeId, String jurisdiction, String workType, String roleCategory, SecurityClassificationEnum securityClassification, OffsetDateTime dueDateTime, List<CreateTaskRequestTaskPermissionsInner> permissions) {
    this.type = type;
    this.name = name;
    this.created = created;
    this.executionType = executionType;
    this.caseId = caseId;
    this.caseTypeId = caseTypeId;
    this.jurisdiction = jurisdiction;
    this.workType = workType;
    this.roleCategory = roleCategory;
    this.securityClassification = securityClassification;
    this.dueDateTime = dueDateTime;
    this.permissions = permissions;
  }

  public CreateTaskRequestTask externalTaskId(UUID externalTaskId) {
    this.externalTaskId = externalTaskId;
    return this;
  }

  /**
   * external task identifier.
   * @return externalTaskId
  */
  
  @Schema(name = "external_task_id", description = "external task identifier.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("external_task_id")
  public UUID getExternalTaskId() {
    return externalTaskId;
  }

  public void setExternalTaskId(UUID externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  public CreateTaskRequestTask type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Service-defined task type identifier.
   * @return type
  */
  @NotNull
  @Schema(name = "type", description = "Service-defined task type identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CreateTaskRequestTask name(String name) {
    this.name = name;
    return this;
  }

  /**
   * User-facing task name.
   * @return name
  */
  @NotNull
  @Schema(name = "name", description = "User-facing task name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CreateTaskRequestTask title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Optional display title.
   * @return title
  */
  
  @Schema(name = "title", description = "Optional display title.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public CreateTaskRequestTask assignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  /**
   * Initial Assignee IDAM ID.
   * @return assignee
  */
  
  @Schema(name = "assignee", description = "Initial Assignee IDAM ID.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("assignee")
  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public CreateTaskRequestTask created(OffsetDateTime created) {
    this.created = created;
    return this;
  }

  /**
   * Creation timestamp.
   * @return created
  */
  @NotNull
  @Schema(name = "created", description = "Creation timestamp.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("created")
  public OffsetDateTime getCreated() {
    return created;
  }

  public void setCreated(OffsetDateTime created) {
    this.created = created;
  }

  public CreateTaskRequestTask executionType(ExecutionTypeEnum executionType) {
    this.executionType = executionType;
    return this;
  }

  /**
   * Execution category.
   * @return executionType
  */
  @NotNull
  @Schema(name = "execution_type", description = "Execution category.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("execution_type")
  public ExecutionTypeEnum getExecutionType() {
    return executionType;
  }

  public void setExecutionType(ExecutionTypeEnum executionType) {
    this.executionType = executionType;
  }

  public CreateTaskRequestTask caseId(String caseId) {
    this.caseId = caseId;
    return this;
  }

  /**
   * CCD case reference.
   * @return caseId
  */
  @NotNull
  @Schema(name = "case_id", description = "CCD case reference.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("case_id")
  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public CreateTaskRequestTask caseTypeId(String caseTypeId) {
    this.caseTypeId = caseTypeId;
    return this;
  }

  /**
   * CCD case type identifier.
   * @return caseTypeId
  */
  @NotNull
  @Schema(name = "case_type_id", description = "CCD case type identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("case_type_id")
  public String getCaseTypeId() {
    return caseTypeId;
  }

  public void setCaseTypeId(String caseTypeId) {
    this.caseTypeId = caseTypeId;
  }

  public CreateTaskRequestTask caseCategory(String caseCategory) {
    this.caseCategory = caseCategory;
    return this;
  }

  /**
   * Primary case management category.
   * @return caseCategory
  */
  
  @Schema(name = "case_category", description = "Primary case management category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("case_category")
  public String getCaseCategory() {
    return caseCategory;
  }

  public void setCaseCategory(String caseCategory) {
    this.caseCategory = caseCategory;
  }

  public CreateTaskRequestTask caseName(String caseName) {
    this.caseName = caseName;
    return this;
  }

  /**
   * Human-readable case name.
   * @return caseName
  */
  
  @Schema(name = "case_name", description = "Human-readable case name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("case_name")
  public String getCaseName() {
    return caseName;
  }

  public void setCaseName(String caseName) {
    this.caseName = caseName;
  }

  public CreateTaskRequestTask jurisdiction(String jurisdiction) {
    this.jurisdiction = jurisdiction;
    return this;
  }

  /**
   * CCD jurisdiction identifier.
   * @return jurisdiction
  */
  @NotNull
  @Schema(name = "jurisdiction", description = "CCD jurisdiction identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("jurisdiction")
  public String getJurisdiction() {
    return jurisdiction;
  }

  public void setJurisdiction(String jurisdiction) {
    this.jurisdiction = jurisdiction;
  }

  public CreateTaskRequestTask region(String region) {
    this.region = region;
    return this;
  }

  /**
   * Case management region identifier.
   * @return region
  */
  
  @Schema(name = "region", description = "Case management region identifier.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("region")
  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public CreateTaskRequestTask location(String location) {
    this.location = location;
    return this;
  }

  /**
   * Base location identifier.
   * @return location
  */
  
  @Schema(name = "location", description = "Base location identifier.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public CreateTaskRequestTask workType(String workType) {
    this.workType = workType;
    return this;
  }

  /**
   * Work type identifier.
   * @return workType
  */
  @NotNull
  @Schema(name = "work_type", description = "Work type identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("work_type")
  public String getWorkType() {
    return workType;
  }

  public void setWorkType(String workType) {
    this.workType = workType;
  }

  public CreateTaskRequestTask roleCategory(String roleCategory) {
    this.roleCategory = roleCategory;
    return this;
  }

  /**
   * Role category for the task.
   * @return roleCategory
  */
  @NotNull
  @Schema(name = "role_category", description = "Role category for the task.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role_category")
  public String getRoleCategory() {
    return roleCategory;
  }

  public void setRoleCategory(String roleCategory) {
    this.roleCategory = roleCategory;
  }

  public CreateTaskRequestTask securityClassification(SecurityClassificationEnum securityClassification) {
    this.securityClassification = securityClassification;
    return this;
  }

  /**
   * Security classification.
   * @return securityClassification
  */
  @NotNull
  @Schema(name = "security_classification", description = "Security classification.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("security_classification")
  public SecurityClassificationEnum getSecurityClassification() {
    return securityClassification;
  }

  public void setSecurityClassification(SecurityClassificationEnum securityClassification) {
    this.securityClassification = securityClassification;
  }

  public CreateTaskRequestTask description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Task description text.
   * @return description
  */
  
  @Schema(name = "description", description = "Task description text.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public CreateTaskRequestTask dueDateTime(OffsetDateTime dueDateTime) {
    this.dueDateTime = dueDateTime;
    return this;
  }

  /**
   * Target due date/time.
   * @return dueDateTime
  */
  @NotNull
  @Schema(name = "due_date_time", description = "Target due date/time.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("due_date_time")
  public OffsetDateTime getDueDateTime() {
    return dueDateTime;
  }

  public void setDueDateTime(OffsetDateTime dueDateTime) {
    this.dueDateTime = dueDateTime;
  }

  public CreateTaskRequestTask priorityDate(OffsetDateTime priorityDate) {
    this.priorityDate = priorityDate;
    return this;
  }

  /**
   * Business priority date/time.
   * @return priorityDate
  */
  
  @Schema(name = "priority_date", description = "Business priority date/time.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("priority_date")
  public OffsetDateTime getPriorityDate() {
    return priorityDate;
  }

  public void setPriorityDate(OffsetDateTime priorityDate) {
    this.priorityDate = priorityDate;
  }

  public CreateTaskRequestTask majorPriority(Integer majorPriority) {
    this.majorPriority = majorPriority;
    return this;
  }

  /**
   * Major priority level.
   * @return majorPriority
  */
  
  @Schema(name = "major_priority", description = "Major priority level.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("major_priority")
  public Integer getMajorPriority() {
    return majorPriority;
  }

  public void setMajorPriority(Integer majorPriority) {
    this.majorPriority = majorPriority;
  }

  public CreateTaskRequestTask minorPriority(Integer minorPriority) {
    this.minorPriority = minorPriority;
    return this;
  }

  /**
   * Minor priority level.
   * @return minorPriority
  */
  
  @Schema(name = "minor_priority", description = "Minor priority level.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minor_priority")
  public Integer getMinorPriority() {
    return minorPriority;
  }

  public void setMinorPriority(Integer minorPriority) {
    this.minorPriority = minorPriority;
  }

  public CreateTaskRequestTask locationName(String locationName) {
    this.locationName = locationName;
    return this;
  }

  /**
   * Location display name.
   * @return locationName
  */
  
  @Schema(name = "location_name", description = "Location display name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location_name")
  public String getLocationName() {
    return locationName;
  }

  public void setLocationName(String locationName) {
    this.locationName = locationName;
  }

  public CreateTaskRequestTask regionName(String regionName) {
    this.regionName = regionName;
    return this;
  }

  /**
   * Region display name.
   * @return regionName
  */
  
  @Schema(name = "region_name", description = "Region display name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("region_name")
  public String getRegionName() {
    return regionName;
  }

  public void setRegionName(String regionName) {
    this.regionName = regionName;
  }

  public CreateTaskRequestTask taskSystem(TaskSystemEnum taskSystem) {
    this.taskSystem = taskSystem;
    return this;
  }

  /**
   * Task origin system.
   * @return taskSystem
  */
  
  @Schema(name = "task_system", description = "Task origin system.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("task_system")
  public TaskSystemEnum getTaskSystem() {
    return taskSystem;
  }

  public void setTaskSystem(TaskSystemEnum taskSystem) {
    this.taskSystem = taskSystem;
  }

  public CreateTaskRequestTask additionalProperties(Map<String, String> additionalProperties) {
    this.additionalProperties = additionalProperties;
    return this;
  }

  public CreateTaskRequestTask putAdditionalPropertiesItem(String key, String additionalPropertiesItem) {
    if (this.additionalProperties == null) {
      this.additionalProperties = new HashMap<>();
    }
    this.additionalProperties.put(key, additionalPropertiesItem);
    return this;
  }

  /**
   * Free-form metadata.
   * @return additionalProperties
  */
  
  @Schema(name = "additional_properties", description = "Free-form metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("additional_properties")
  public Map<String, String> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Map<String, String> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public CreateTaskRequestTask permissions(List<CreateTaskRequestTaskPermissionsInner> permissions) {
    this.permissions = permissions;
    return this;
  }

  public CreateTaskRequestTask addPermissionsItem(CreateTaskRequestTaskPermissionsInner permissionsItem) {
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
  public List<CreateTaskRequestTaskPermissionsInner> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<CreateTaskRequestTaskPermissionsInner> permissions) {
    this.permissions = permissions;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateTaskRequestTask createTaskRequestTask = (CreateTaskRequestTask) o;
    return Objects.equals(this.externalTaskId, createTaskRequestTask.externalTaskId) &&
        Objects.equals(this.type, createTaskRequestTask.type) &&
        Objects.equals(this.name, createTaskRequestTask.name) &&
        Objects.equals(this.title, createTaskRequestTask.title) &&
        Objects.equals(this.assignee, createTaskRequestTask.assignee) &&
        Objects.equals(this.created, createTaskRequestTask.created) &&
        Objects.equals(this.executionType, createTaskRequestTask.executionType) &&
        Objects.equals(this.caseId, createTaskRequestTask.caseId) &&
        Objects.equals(this.caseTypeId, createTaskRequestTask.caseTypeId) &&
        Objects.equals(this.caseCategory, createTaskRequestTask.caseCategory) &&
        Objects.equals(this.caseName, createTaskRequestTask.caseName) &&
        Objects.equals(this.jurisdiction, createTaskRequestTask.jurisdiction) &&
        Objects.equals(this.region, createTaskRequestTask.region) &&
        Objects.equals(this.location, createTaskRequestTask.location) &&
        Objects.equals(this.workType, createTaskRequestTask.workType) &&
        Objects.equals(this.roleCategory, createTaskRequestTask.roleCategory) &&
        Objects.equals(this.securityClassification, createTaskRequestTask.securityClassification) &&
        Objects.equals(this.description, createTaskRequestTask.description) &&
        Objects.equals(this.dueDateTime, createTaskRequestTask.dueDateTime) &&
        Objects.equals(this.priorityDate, createTaskRequestTask.priorityDate) &&
        Objects.equals(this.majorPriority, createTaskRequestTask.majorPriority) &&
        Objects.equals(this.minorPriority, createTaskRequestTask.minorPriority) &&
        Objects.equals(this.locationName, createTaskRequestTask.locationName) &&
        Objects.equals(this.regionName, createTaskRequestTask.regionName) &&
        Objects.equals(this.taskSystem, createTaskRequestTask.taskSystem) &&
        Objects.equals(this.additionalProperties, createTaskRequestTask.additionalProperties) &&
        Objects.equals(this.permissions, createTaskRequestTask.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalTaskId, type, name, title, assignee, created, executionType, caseId, caseTypeId, caseCategory, caseName, jurisdiction, region, location, workType, roleCategory, securityClassification, description, dueDateTime, priorityDate, majorPriority, minorPriority, locationName, regionName, taskSystem, additionalProperties, permissions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTaskRequestTask {\n");
    sb.append("    externalTaskId: ").append(toIndentedString(externalTaskId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    assignee: ").append(toIndentedString(assignee)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    executionType: ").append(toIndentedString(executionType)).append("\n");
    sb.append("    caseId: ").append(toIndentedString(caseId)).append("\n");
    sb.append("    caseTypeId: ").append(toIndentedString(caseTypeId)).append("\n");
    sb.append("    caseCategory: ").append(toIndentedString(caseCategory)).append("\n");
    sb.append("    caseName: ").append(toIndentedString(caseName)).append("\n");
    sb.append("    jurisdiction: ").append(toIndentedString(jurisdiction)).append("\n");
    sb.append("    region: ").append(toIndentedString(region)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    workType: ").append(toIndentedString(workType)).append("\n");
    sb.append("    roleCategory: ").append(toIndentedString(roleCategory)).append("\n");
    sb.append("    securityClassification: ").append(toIndentedString(securityClassification)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    dueDateTime: ").append(toIndentedString(dueDateTime)).append("\n");
    sb.append("    priorityDate: ").append(toIndentedString(priorityDate)).append("\n");
    sb.append("    majorPriority: ").append(toIndentedString(majorPriority)).append("\n");
    sb.append("    minorPriority: ").append(toIndentedString(minorPriority)).append("\n");
    sb.append("    locationName: ").append(toIndentedString(locationName)).append("\n");
    sb.append("    regionName: ").append(toIndentedString(regionName)).append("\n");
    sb.append("    taskSystem: ").append(toIndentedString(taskSystem)).append("\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
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

