package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModel;
import org.springframework.format.annotation.DateTimeFormat;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMAT;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings({"PMD.LawOfDemeter","PMD.TooManyFields","PMD.ExcessiveParameterList"})
@ApiModel("Task")
public class MappedTask {

    private String id;
    private String name;
    private String assignee;
    private String type;
    private String taskState;
    private String taskSystem;
    private String securityClassification;
    private String taskTitle;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private String createdDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private String dueDate;
    private String locationName;
    private String location;
    private String executionType;
    private String jurisdiction;
    private String region;
    private String caseTypeId;
    private String caseId;
    private String caseCategory;
    private String caseName;
    private Boolean autoAssigned;

    public MappedTask() {
        //Default constructor for deserialization
        super();
    }

    public MappedTask(String id,
                      String name,
                      String type,
                      String taskState,
                      String taskSystem,
                      String securityClassification,
                      String taskTitle,
                      String createdDate,
                      String dueDate,
                      String assignee,
                      Boolean autoAssigned,
                      String executionType,
                      String jurisdiction,
                      String region,
                      String location,
                      String locationName,
                      String caseTypeId,
                      String caseId,
                      String caseCategory,
                      String caseName
    ) {
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

    public Boolean getAutoAssigned() {
        return autoAssigned;
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

    public String getCreatedDate() {
        return createdDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getLocationName() {
        return locationName;
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

}

