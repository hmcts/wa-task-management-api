package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaTask {

    private String id;
    private String name;
    private String assignee;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CamundaTime.CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime created;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CamundaTime.CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime due;
    private String description;
    private String owner;
    private String formKey;
    private String processInstanceId;

    private CamundaTask() {
        //Hidden constructor
        super();
    }

    public CamundaTask(String id,
                       String name,
                       String assignee,
                       ZonedDateTime created,
                       ZonedDateTime due,
                       String description,
                       String owner,
                       String formKey,
                       String processInstanceId
    ) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.created = created;
        this.due = due;
        this.description = description;
        this.owner = owner;
        this.formKey = formKey;
        this.processInstanceId = processInstanceId;
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

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
