package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;
import java.util.Objects;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyFields",
    "PMD.ExcessiveParameterList", "PMD.ShortClassName", "PMD.LinguisticNaming"})
@ApiModel("Task")
public class Task {
    @ApiModelProperty(
        required = true,
        notes = "Unique identifier for the task"
    )
    private String id;
    @ApiModelProperty(
        required = true,
        notes = "Name of the task assigned in the process model"
    )
    private String name;
    @ApiModelProperty(
        required = true,
        notes = "The single user who has been assigned this task i.e. IDAM ID"
    )
    private String assignee;
    @ApiModelProperty(
        required = true,
        notes = "Unique identifier for the conceptual business task"
    )
    private String type;
    @ApiModelProperty(
        required = true,
        notes = "unconfigured, unassigned, configured, assigned, referred, completed, cancelled"
    )
    private String taskState;
    @ApiModelProperty(
        required = true,
        notes = " Code indicating the system which is responsible for this task. For MVP will be always SELF"
    )
    private String taskSystem;
    @ApiModelProperty(
        required = true,
        notes = "The security classification of the main business entity this task relates to."
                + " Can be PUBLIC, PRIVATE, RESTRICTED"
    )
    private String securityClassification;
    @ApiModelProperty(
        required = true,
        notes = "Task title to display in task list UI"
    )
    private String taskTitle;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @ApiModelProperty(
        example = "2020-09-05T14:47:01.250542+01:00",
        notes = "Optional due date for the task that will be created"
    )
    private ZonedDateTime createdDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @ApiModelProperty(
        example = "2020-09-05T14:47:01.250542+01:00",
        notes = "Optional due date for the task that will be created"
    )
    private ZonedDateTime dueDate;
    @ApiModelProperty(required = true,
        notes = "`location to display in task list UI"
    )
    private String locationName;
    @ApiModelProperty(required = true,
        notes = "The ePims ID for the physical location"
    )
    private String location;
    @ApiModelProperty(required = true,
        notes = "Indicator to the user interface of how this task is to be executed. "
                + "For MVP, this will always be \"Case Management Task\""
    )
    private String executionType;
    @ApiModelProperty(required = true,
        notes = "For MVP, will always be \"IA\""
    )
    private String jurisdiction;
    @ApiModelProperty(required = true,
        notes = " The region ID. For IAC is always \"1\" (national)"
    )
    private String region;
    @ApiModelProperty(required = true,
        notes = " The CCD case type ID"
    )
    private String caseTypeId;
    @ApiModelProperty(required = true,
        notes = " Case ID to display in task list UI"
    )
    private String caseId;
    @ApiModelProperty(required = true,
        notes = " Case category  to display in task list UI"
    )
    private String caseCategory;
    @ApiModelProperty(required = true,
        notes = " Case name to display in task list UI"
    )
    private String caseName;
    @ApiModelProperty(required = true,
        notes = "If TRUE then task was auto-assigned, otherwise FALSE"
    )
    private boolean autoAssigned;

    @ApiModelProperty(required = false,
        notes = "boolean to show if a warning is applied to task by a service task in a subprocess")
    private Boolean warnings;

    @ApiModelProperty(required = false,
        notes = "A list of values containing a warning code and warning text")
    private WarningValues warningList;

    @ApiModelProperty(required = false,
        notes = "A value describing the category of the case, for IA, it has the same value as the AppealType field")
    private String caseManagementCategory;

    @ApiModelProperty(required = true,
        notes = "A value containing the work type id for this task, for IA")
    private String workTypeId;

    @ApiModelProperty(required = true,
        notes = "A value describing the task permissions")
    private TaskPermissions permissions;
    @ApiModelProperty(required = true,
        notes = "A value describing to users what they should do next")
    private String description;

    @ApiModelProperty(required = true,
        notes = "A value describing the role category")
    private String roleCategory;

    private Task() {
        //Hidden constructor
        super();
    }

    public Task(String id,
                String name,
                String type,
                String taskState,
                String taskSystem,
                String securityClassification,
                String taskTitle,
                ZonedDateTime createdDate,
                ZonedDateTime dueDate,
                String assignee,
                boolean autoAssigned,
                String executionType,
                String jurisdiction,
                String region,
                String location,
                String locationName,
                String caseTypeId,
                String caseId,
                String caseCategory,
                String caseName,
                Boolean warnings,
                WarningValues warningList,
                String caseManagementCategory,
                String workTypeId,
                TaskPermissions taskPermissions,
                String description,
                String roleCategory
    ) {
        Objects.requireNonNull(id, "taskId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        this.id = id;
        this.executionType = executionType;
        this.name = name;
        this.assignee = assignee;
        this.autoAssigned = autoAssigned;
        this.caseCategory = caseCategory;
        this.caseId = caseId;
        this.type = type;
        this.taskState = taskState;
        this.taskSystem = taskSystem;
        this.locationName = locationName;
        this.securityClassification = securityClassification;
        this.taskTitle = taskTitle;
        this.createdDate = createdDate;
        this.dueDate = dueDate;
        this.caseTypeId = caseTypeId;
        this.caseName = caseName;
        this.jurisdiction = jurisdiction;
        this.region = region;
        this.location = location;
        this.warnings = warnings;
        this.warningList = warningList;
        this.caseManagementCategory = caseManagementCategory;
        this.workTypeId = workTypeId;
        this.permissions = taskPermissions;
        this.description = description;
        this.roleCategory = roleCategory;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getType() {
        return type;
    }

    public String getTaskState() {
        return taskState;
    }

    public String getTaskSystem() {
        return taskSystem;
    }

    public String getSecurityClassification() {
        return securityClassification;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocation() {
        return location;
    }

    public String getExecutionType() {
        return executionType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getRegion() {
        return region;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCaseCategory() {
        return caseCategory;
    }

    public String getCaseName() {
        return caseName;
    }

    public boolean isAutoAssigned() {
        return autoAssigned;
    }

    public Boolean getWarnings() {
        return warnings;
    }

    public WarningValues getWarningList() {
        return warningList;
    }

    public String getCaseManagementCategory() {
        return caseManagementCategory;
    }

    public String getWorkTypeId() {
        return workTypeId;
    }

    public TaskPermissions getPermissions() {
        return permissions;
    }

    public String getDescription() {
        return description;
    }

    public String getRoleCategory() {
        return roleCategory;
    }
}

