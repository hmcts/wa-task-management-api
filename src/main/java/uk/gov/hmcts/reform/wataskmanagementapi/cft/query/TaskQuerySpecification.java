package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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
    private static final String ROLE_CATEGORY = "roleCategory";
    public static final int SINGLE_ELEMENT = 1;

    private TaskQuerySpecification() {
        // avoid creating object
    }

    public static Predicate searchByState(List<CFTTaskState> cftTaskStates,
                                          CriteriaBuilder builder,
                                          Root<TaskResource> root) {
        if (isEmpty(cftTaskStates)) {
            return builder.conjunction();
        } else if (cftTaskStates.size() == SINGLE_ELEMENT) {
            return builder.equal(root.get(STATE), cftTaskStates.get(0));
        }
        return builder.in(root.get(STATE)).value(cftTaskStates);
    }

    public static Predicate searchByJurisdiction(List<String> jurisdictions,
                                                 CriteriaBuilder builder,
                                                 Root<TaskResource> root) {
        if (isEmpty(jurisdictions)) {
            return builder.conjunction();
        } else if (hasSingleElement(jurisdictions)) {
            return builder.equal(root.get(JURISDICTION), jurisdictions.get(0));
        }
        return builder.in(root.get(JURISDICTION)).value(jurisdictions);
    }

    public static Predicate searchByLocation(List<String> locations,
                                             CriteriaBuilder builder,
                                             Root<TaskResource> root) {
        if (isEmpty(locations)) {
            return builder.conjunction();
        } else if (hasSingleElement(locations)) {
            return builder.equal(root.get(LOCATION), locations.get(0));
        }
        return builder.in(root.get(LOCATION)).value(locations);
    }

    public static Predicate searchByCaseId(String caseId,
                                           CriteriaBuilder builder,
                                           Root<TaskResource> root) {
        return builder.equal(root.get(CASE_ID), caseId);
    }

    public static Predicate searchByCaseIds(List<String> caseIds,
                                            CriteriaBuilder builder,
                                            Root<TaskResource> root) {
        if (isEmpty(caseIds)) {
            return builder.conjunction();
        } else if (hasSingleElement(caseIds)) {
            return builder.equal(root.get(CASE_ID), caseIds.get(0));
        }
        return builder.in(root.get(CASE_ID)).value(caseIds);
    }

    public static Predicate searchByUser(List<String> users,
                                         CriteriaBuilder builder,
                                         Root<TaskResource> root) {
        if (isEmpty(users)) {
            return builder.conjunction();
        } else if (hasSingleElement(users)) {
            return builder.equal(root.get(ASSIGNEE), users.get(0));
        }
        return builder.in(root.get(ASSIGNEE)).value(users);
    }

    public static Predicate searchByTaskIds(List<String> taskIds,
                                            CriteriaBuilder builder,
                                            Root<TaskResource> root) {
        if (isEmpty(taskIds)) {
            return builder.conjunction();
        } else if (hasSingleElement(taskIds)) {
            return builder.equal(root.get(TASK_ID), taskIds.get(0));
        }
        return builder.in(root.get(TASK_ID)).value(taskIds);
    }

    public static Predicate searchByTaskTypes(List<String> taskTypes,
                                              CriteriaBuilder builder,
                                              Root<TaskResource> root) {
        if (isEmpty(taskTypes)) {
            return builder.conjunction();
        } else if (hasSingleElement(taskTypes)) {
            return builder.equal(root.get(TASK_TYPE), taskTypes.get(0));
        }
        return builder.in(root.get(TASK_TYPE)).value(taskTypes);
    }

    public static Predicate searchByWorkType(List<String> workTypes,
                                             CriteriaBuilder builder,
                                             Root<TaskResource> root) {
        if (isEmpty(workTypes)) {
            return builder.conjunction();
        } else if (hasSingleElement(workTypes)) {
            return builder.equal(root.get(WORK_TYPE).get(WORK_TYPE_ID), workTypes.get(0));
        }
        return builder.in(root.get(WORK_TYPE).get(WORK_TYPE_ID)).value(workTypes);
    }

    public static Predicate searchByRoleCategory(List<RoleCategory> roleCategories,
                                                 CriteriaBuilder builder,
                                                 Root<TaskResource> root) {
        List<String> roleCategoryTexts = Stream.ofNullable(roleCategories)
            .flatMap(Collection::stream)
            .map(Objects::toString).collect(Collectors.toList());
        if (isEmpty(roleCategoryTexts)) {
            return builder.conjunction();
        } else if (hasSingleElement(roleCategoryTexts)) {
            return builder.equal(root.get(ROLE_CATEGORY), roleCategoryTexts.get(0));
        }
        return builder.in(root.get(ROLE_CATEGORY)).value(roleCategoryTexts);
    }

    private static boolean hasSingleElement(List<?> list) {
        return list.size() == SINGLE_ELEMENT;
    }
}
