package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyFields",
    "PMD.ExcessiveParameterList", "PMD.ShortClassName", "PMD.LinguisticNaming"})
@Schema(allowableValues = "Task")
public class Task {
    @Schema(
        required = true,
        description = "Unique identifier for the task"
    )
    private final String id;
    @Schema(
        required = true,
        description = "Name of the task assigned in the process model"
    )
    private final String name;
    @Schema(
        required = true,
        description = "The single user who has been assigned this task i.e. IDAM ID"
    )
    private final String assignee;
    @Schema(
        required = true,
        description = "Unique identifier for the conceptual business task"
    )
    private final String type;
    @Schema(
        required = true,
        description = "unconfigured, unassigned, configured, assigned, referred, completed, cancelled"
    )
    private final String taskState;
    @Schema(
        required = true,
        description = " Code indicating the system which is responsible for this task. For MVP will be always SELF"
    )
    private final String taskSystem;
    @Schema(
        required = true,
        description = "The security classification of the main business entity this task relates to."
            + " Can be PUBLIC, PRIVATE, RESTRICTED"
    )
    private final String securityClassification;
    @Schema(
        required = true,
        description = "Task title to display in task list UI"
    )
    private final String taskTitle;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @Schema(
        example = "2020-09-05T14:47:01.250542+01:00",
        description = "Optional due date for the task that will be created"
    )
    private final ZonedDateTime createdDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @Schema(
        example = "2020-09-05T14:47:01.250542+01:00",
        description = "Optional due date for the task that will be created"
    )
    private final ZonedDateTime dueDate;
    @Schema(required = true,
        description = "`location to display in task list UI"
    )
    private final String locationName;
    @Schema(required = true,
        description = "The ePims ID for the physical location"
    )
    private final String location;
    @Schema(required = true,
        description = "Indicator to the user interface of how this task is to be executed. "
            + "For MVP, this will always be \"Case Management Task\""
    )
    private final String executionType;
    @Schema(required = true,
        description = "For MVP, will always be \"IA\""
    )
    private final String jurisdiction;
    @Schema(required = true,
        description = " The region ID. For IAC is always \"1\" (national)"
    )
    private final String region;
    @Schema(required = true,
        description = " The CCD case type ID"
    )
    private final String caseTypeId;
    @Schema(required = true,
        description = " Case ID to display in task list UI"
    )
    private final String caseId;
    @Schema(required = true,
        description = " Case category  to display in task list UI"
    )
    private final String caseCategory;
    @Schema(required = true,
        description = " Case name to display in task list UI"
    )
    private final String caseName;
    @Schema(required = true,
        description = "If TRUE then task was auto-assigned, otherwise FALSE"
    )
    private final boolean autoAssigned;

    @Schema(
        description = "boolean to show if a warning is applied to task by a service task in a subprocess")
    private final Boolean warnings;

    @Schema(
        description = "A list of values containing a warning code and warning text")
    private final WarningValues warningList;

    @Schema(description = "A value describing the category of the case, for IA, "
        + "it has the same value as the AppealType field")
    private final String caseManagementCategory;

    @Schema(required = true,
        description = "A value containing the work type id for this task, for IA")
    private final String workTypeId;

    @Schema(required = true,
        description = "A value containing the work type label for this task, for IA")
    private final String workTypeLabel;

    @Schema(required = true,
        description = "A value describing the task permissions")
    private final TaskPermissions permissions;
    @Schema(required = true,
        description = "A value describing to users what they should do next")
    private final String description;

    @Schema(required = true,
        description = "A value describing the role category")
    private final String roleCategory;

    @Schema(required = true,
        description = "A value describing the additional properties")
    private final Map<String, String> additionalProperties;
    private final String nextHearingId;
    private final ZonedDateTime nextHearingDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    @Schema(
        example = "2020-09-05T14:47:01.250542+01:00",
        description = "Optional reconfigure request time"
    )
    private ZonedDateTime reconfigureRequestTime;

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
                String workTypeLabel,
                TaskPermissions taskPermissions,
                String roleCategory,
                String description,
                Map<String, String> additionalProperties,
                String nextHearingId,
                ZonedDateTime nextHearingDate) {
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
        this.workTypeLabel = workTypeLabel;
        this.permissions = taskPermissions;
        this.roleCategory = roleCategory;
        this.description = description;
        this.additionalProperties = additionalProperties;
        this.nextHearingId = nextHearingId;
        this.nextHearingDate = nextHearingDate;
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
                String workTypeLabel,
                TaskPermissions taskPermissions,
                String roleCategory,
                String description,
                Map<String, String> additionalProperties,
                String nextHearingId,
                ZonedDateTime nextHearingDate,
                ZonedDateTime reconfigureRequestTime) {
        this(id,
            name,
            type,
            taskState,
            taskSystem,
            securityClassification,
            taskTitle,
            createdDate,
            dueDate,
            assignee,
            autoAssigned,
            executionType,
            jurisdiction,
            region,
            location,
            locationName,
            caseTypeId,
            caseId,
            caseCategory,
            caseName,
            warnings,
            warningList,
            caseManagementCategory,
            workTypeId,
            workTypeLabel,
            taskPermissions,
            roleCategory,
            description,
            additionalProperties,
            nextHearingId,
            nextHearingDate);
        this.reconfigureRequestTime = reconfigureRequestTime;
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

    public String getWorkTypeLabel() {
        return workTypeLabel;
    }

    public TaskPermissions getPermissions() {
        return permissions;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public ZonedDateTime getReconfigureRequestTime() {
        return reconfigureRequestTime;
    }
    public String getNextHearingId() {
        return nextHearingId;
    }

    public ZonedDateTime getNextHearingDate() {
        return nextHearingDate;
    }
}

