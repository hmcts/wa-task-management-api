package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import javax.persistence.criteria.Join;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@SuppressWarnings({"PMD.TooManyMethods"})
public final class TaskQuerySpecification {

    private static final String STATE = "state";
    private static final String LOCATION = "location";
    private static final String TASK_ID = "taskId";
    private static final String TASK_TYPE = "taskType";
    private static final String ASSIGNEE = "assignee";
    private static final String CASE_ID = "caseId";
    private static final String JURISDICTION = "jurisdiction";
    private static final String WORK_TYPE = "workTypeResource";
    private static final String WORK_TYPE_ID = "id";
    private static final String OWN_ATTRIBUTE = "own";
    private static final String TASK_ROLE_RESOURCES = "taskRoleResources";
    private static final String ROLE_CATEGORY = "roleCategory";


    private TaskQuerySpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> searchByState(List<CFTTaskState> cftTaskStates) {
        if (isEmpty(cftTaskStates)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(STATE))
            .value(cftTaskStates);
    }

    public static Specification<TaskResource> searchByJurisdiction(List<String> jurisdictions) {
        if (isEmpty(jurisdictions)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(JURISDICTION))
            .value(jurisdictions);
    }

    public static Specification<TaskResource> searchByLocation(List<String> locations) {
        if (isEmpty(locations)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(LOCATION))
            .value(locations);
    }

    public static Specification<TaskResource> searchByCaseId(String caseId) {
        return (root, query, builder) -> builder.equal(root.get(CASE_ID), caseId);
    }

    public static Specification<TaskResource> searchByCaseIds(List<String> caseIds) {
        if (isEmpty(caseIds)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(CASE_ID))
            .value(caseIds);
    }

    public static Specification<TaskResource> searchByUser(List<String> users) {
        if (isEmpty(users)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(ASSIGNEE))
            .value(users);
    }

    public static Specification<TaskResource> searchByTaskId(String taskId) {
        return (root, query, builder) -> builder.equal(root.get(TASK_ID), taskId);
    }

    public static Specification<TaskResource> searchByTaskTypes(List<String> taskTypes) {
        if (isEmpty(taskTypes)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(TASK_TYPE))
            .value(taskTypes);
    }

    public static Specification<TaskResource> searchByWorkType(List<String> workTypes) {
        if (isEmpty(workTypes)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(WORK_TYPE).get(WORK_TYPE_ID))
            .value(workTypes);
    }

    public static Specification<TaskResource> searchByRoleCategory(List<String> roleCategories) {
        if (isEmpty(roleCategories)) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.in(root.get(ROLE_CATEGORY))
            .value(roleCategories);
    }

    //TODO: Do we need to add the search task request to this?
    public static Specification<TaskResource> searchByAvailableTasksOnly() {
        return (root, query, builder) ->  {
            final Join<TaskResource, TaskRoleResource> taskRoleResources = root.join(TASK_ROLE_RESOURCES);
            return builder.isTrue(taskRoleResources.get(OWN_ATTRIBUTE));
        };
    }

}
