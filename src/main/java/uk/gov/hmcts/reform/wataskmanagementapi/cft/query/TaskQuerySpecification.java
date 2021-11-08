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
    public static final String WORK_TYPE = "workTypeResource";

    private TaskQuerySpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> searchByState(List<CFTTaskState> cftTaskStates) {
        return (root, query, builder) -> builder.in(root.get(STATE))
            .value(cftTaskStates);
    }

    public static Specification<TaskResource> searchByJurisdiction(List<String> jurisdictions) {
        return (root, query, builder) -> builder.in(root.get(JURISDICTION))
            .value(jurisdictions);
    }

    public static Specification<TaskResource> searchByLocation(List<String> locations) {
        return (root, query, builder) -> builder.in(root.get(LOCATION))
            .value(locations);
    }

    public static Specification<TaskResource> searchByCaseId(List<String> caseIds) {
        return (root, query, builder) -> builder.in(root.get(CASE_ID))
            .value(caseIds);
    }

    public static Specification<TaskResource> searchByUser(List<String> users) {
        return (root, query, builder) -> builder.in(root.get(ASSIGNEE))
            .value(users);
    }

    public static Specification<TaskResource> searchByTaskId(String taskId) {
        return (root, query, builder) -> builder.equal(root.get(TASK_ID), taskId);
    }

    public static Specification<TaskResource> searchByTaskTypes(List<String> taskTypes) {
        return (root, query, builder) -> builder.in(root.get(TASK_TYPE))
            .value(taskTypes);
    }

    public static Specification<TaskResource> searchByWorkType(List<String> workTypes) {
        return (root, query, builder) -> builder.in(root.get(WORK_TYPE))
            .value(workTypes);
    }

}
