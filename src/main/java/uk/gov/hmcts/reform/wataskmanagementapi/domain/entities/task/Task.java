package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import io.swagger.annotations.ApiModel;

import java.time.ZonedDateTime;

@SuppressWarnings("PMD.ShortClassName")
@ApiModel("Task")
public class Task {

    private String name;
    private String state;
    private ZonedDateTime dueDate;
    private CaseData caseData;
    private Assignee assignee;

    public Task() {
        //Default constructor for deserialization
        super();
    }

    public Task(String name, String state, ZonedDateTime dueDate, CaseData caseData, Assignee assignee) {
        this.name = name;
        this.state = state;
        this.dueDate = dueDate;
        this.caseData = caseData;
        this.assignee = assignee;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public CaseData getCaseData() {
        return caseData;
    }

    public Assignee getAssignee() {
        return assignee;
    }
}
