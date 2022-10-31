package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildQueryToRetrieveRoleInformation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByRoleCategory;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByWorkType;

@Slf4j
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis",
    "PMD.TooManyMethods",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveImports",
    "PMD.NPathComplexity",
    "PMD.CyclomaticComplexity"
})
public final class TaskSearchQueryBuilder {


    private TaskSearchQueryBuilder() {
        // avoid creating object
    }

    public static Predicate buildTaskSummaryQuery(
        SearchTaskRequest searchTaskRequest,
        List<RoleAssignment> roleAssignments,
        PermissionRequirements permissionsRequired,
        Boolean availableTasksOnly,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        log.debug("Querying with 'available_tasks_only' set to '{}'", availableTasksOnly);
        log.debug("Querying with 'permissions required' set to '{}'", permissionsRequired);

        final Predicate constrainsSpec =
            buildApplicationConstraints(searchTaskRequest, builder, root);

        final Predicate roleAssignmentSpec = buildRoleAssignmentConstraints(
            permissionsRequired,
            roleAssignments,
            builder,
            root
        );

        return builder.and(constrainsSpec, roleAssignmentSpec);
    }

    public static Predicate buildTaskQuery(
        List<String> taskIds,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        return searchByTaskIds(taskIds, builder, root);
    }

    public static Predicate buildSingleTaskQuery(String taskId,
                                                 List<RoleAssignment> roleAssignments,
                                                 PermissionRequirements permissionsRequired,
                                                 CriteriaBuilder builder,
                                                 Root<TaskResource> root
    ) {
        Predicate roleAssignmentConstraints = buildRoleAssignmentConstraints(
            permissionsRequired,
            roleAssignments,
            builder,
            root
        );

        return builder.and(searchByTaskIds(singletonList(taskId), builder, root), roleAssignmentConstraints);
    }

    public static Predicate buildTaskRolePermissionsQuery(
        String taskId,
        List<RoleAssignment> roleAssignments,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        return builder.and(
            searchByTaskIds(singletonList(taskId), builder, root),
            buildQueryToRetrieveRoleInformation(roleAssignments, builder, root)
        );
    }

    public static Predicate buildQueryForCompletable(
        SearchEventAndCase searchEventAndCase,
        List<RoleAssignment> roleAssignments,
        PermissionRequirements permissionsRequired,
        List<String> taskTypes,
        CriteriaBuilder builder,
        Root<TaskResource> root) {

        ArrayList<Predicate> predicates = new ArrayList<>();

        predicates.add(searchByCaseId(searchEventAndCase.getCaseId(), builder, root));
        predicates.add(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), builder, root));
        predicates.add(searchByTaskTypes(taskTypes, builder, root));
        predicates.add(buildRoleAssignmentConstraints(
            permissionsRequired,
            roleAssignments,
            builder,
            root
        ));
        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private static Predicate buildApplicationConstraints(SearchTaskRequest searchTaskRequest,
                                                         CriteriaBuilder builder,
                                                         Root<TaskResource> root) {

        ArrayList<Predicate> predicates = new ArrayList<>();
        predicates.add(searchByJurisdiction(
            searchTaskRequest.getJurisdictions(),
            builder,
            root
        ));
        predicates.add(searchByState(searchTaskRequest.getCftTaskStates(), builder, root));
        predicates.add(searchByLocation(
            searchTaskRequest.getLocations(),
            builder,
            root
        ));

        predicates.add(searchByCaseIds(
            searchTaskRequest.getCaseIds(),
            builder,
            root
        ));
        predicates.add(searchByUser(
            searchTaskRequest.getUsers(),
            builder,
            root
        ));
        predicates.add(searchByWorkType(
            searchTaskRequest.getWorkTypes(),
            builder,
            root
        ));
        predicates.add(searchByRoleCategory(
            searchTaskRequest.getRoleCategories(),
            builder, root
        ));
        predicates.add(searchByTaskTypes(
            searchTaskRequest.getTaskTypes(),
            builder, root
        ));

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}
