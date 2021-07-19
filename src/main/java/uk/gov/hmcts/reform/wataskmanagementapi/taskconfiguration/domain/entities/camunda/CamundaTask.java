package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMAT;


@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessiveParameterList"})
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
public class CamundaTask {

    private String id;
    private String name;
    @JsonProperty("processInstanceId")
    private String processInstanceId;
    private String assignee;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime created;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime due;
    private String description;
    private String owner;
    private String formKey;

    private CamundaTask() {
        //Hidden constructor
        super();
    }

    public CamundaTask(String id, String processInstanceId, String name) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.name = name;
    }

    public CamundaTask(String id,
                       String name,
                       String processInstanceId,
                       String assignee,
                       ZonedDateTime created,
                       ZonedDateTime due,
                       String description,
                       String owner,
                       String formKey) {
        this.id = id;
        this.name = name;
        this.processInstanceId = processInstanceId;
        this.assignee = assignee;
        this.created = created;
        this.due = due;
        this.description = description;
        this.owner = owner;
        this.formKey = formKey;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getAssignee() {
        return assignee;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getDue() {
        return due;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getFormKey() {
        return formKey;
    }
}
