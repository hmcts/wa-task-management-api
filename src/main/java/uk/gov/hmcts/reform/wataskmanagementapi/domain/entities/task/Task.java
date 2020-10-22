package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import io.swagger.annotations.ApiModel;

import java.time.ZonedDateTime;
import java.util.Objects;

@SuppressWarnings("PMD.ShortClassName")
@ApiModel("Task")
public class Task {

    private String name;
    private String state;
    private ZonedDateTime dueDate;
    private CaseData caseData;
    private Assignee assignee;

    private Task() {
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Task task = (Task) object;
        return Objects.equals(name, task.name)
               && Objects.equals(state, task.state)
               && Objects.equals(dueDate, task.dueDate)
               && Objects.equals(caseData, task.caseData)
               && Objects.equals(assignee, task.assignee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, state, dueDate, caseData, assignee);
    }

    @Override
    public String toString() {
        return "Task{"
               + "name='" + name + '\''
               + ", state='" + state + '\''
               + ", dueDate=" + dueDate
               + ", caseData=" + caseData
               + ", assignee=" + assignee
               + '}';
    }
}
