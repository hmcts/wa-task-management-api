package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import java.util.List;

public final class TaskQuerySpecification {

    public static final String STATE = "state";
    public static final String LOCATION = "location";
    public static final String TASK_ID = "taskId";
    public static final String TASK_TYPE = "taskType";
    public static final String ASSIGNEE = "assignee";
    public static final String CASE_ID = "caseId";
    public static final String JURISDICTION = "jurisdiction";

    private TaskQuerySpecification() {
        // avoid creating object
    }

    protected static Specification<TaskResource> searchByState(List<CFTTaskState> cftTaskStates) {
        return (root, query, builder) -> builder.in(root.get(STATE))
            .value(cftTaskStates);
    }

    protected static Specification<TaskResource> searchByJurisdiction(List<String> jurisdictions) {
        return (root, query, builder) -> builder.in(root.get(JURISDICTION))
            .value(jurisdictions);
    }

    protected static Specification<TaskResource> searchByLocation(List<String> locations) {
        return (root, query, builder) -> builder.in(root.get(LOCATION))
            .value(locations);
    }

    protected static Specification<TaskResource> searchByCaseId(List<String> caseIds) {
        return (root, query, builder) -> builder.in(root.get(CASE_ID))
            .value(caseIds);
    }

    protected static Specification<TaskResource> searchByUser(List<String> users) {
        return (root, query, builder) -> builder.in(root.get(ASSIGNEE))
            .value(users);
    }

    protected static Specification<TaskResource> searchByTaskType(List<String> taskTypes) {
        return (root, query, builder) -> builder.in(root.get(TASK_TYPE))
            .value(taskTypes);
    }

    protected static Specification<TaskResource> searchByTaskId(List<String> taskIds) {
        return (root, query, builder) -> builder.in(root.get(TASK_ID))
            .value(taskIds);
    }

}
