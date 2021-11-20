package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.buildRoleAssignmentConstraints;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.extractWorkType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class TaskResourceSpecification {

    public static final String LOCATION = "location";
    public static final String TASK_ID = "taskId";
    public static final String TASK_TYPE = "taskType";
    public static final String CASE_ID = "caseId";
    public static final String ROLE_NAME = "roleName";
    public static final String WORK_TYPE = "workTypeResource";

    private TaskResourceSpecification() {
        // avoid creating object
    }

    public static Specification<TaskResource> buildTaskQuery(
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {

        return buildApplicationConstraints(searchTaskRequest)
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    public static Specification<TaskResource> buildSingleTaskQuery(String taskId,
                                                                   AccessControlResponse accessControlResponse,
                                                                   List<PermissionTypes> permissionsRequired
    ) {
        return searchByTaskId(taskId)
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }


    public static Specification<TaskResource> buildQueryForCompletable(
        SearchEventAndCase searchEventAndCase, AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired, List<String> taskTypes) {

        return searchByCaseId(searchEventAndCase.getCaseId())
            .and(searchByState(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .and(searchByTaskTypes(taskTypes))
            //.and(searchByUser(List.of(accessControlResponse.getUserInfo().getUid())))
            .and(buildRoleAssignmentConstraints(permissionsRequired, accessControlResponse));
    }

    private static Specification<TaskResource> buildApplicationConstraints(SearchTaskRequest searchTaskRequest) {

        return extractJurisdiction(searchTaskRequest)
            .and(extractState(searchTaskRequest))
            .and(extractLocation(searchTaskRequest))
            .and(extractCaseId(searchTaskRequest))
            .and(extractUser(searchTaskRequest))
            .and(extractWorkType(searchTaskRequest));
    }

}
