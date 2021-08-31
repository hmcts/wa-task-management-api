package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.WORK_TYPES;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
@RequestMapping(path = "/work-types", produces = APPLICATION_JSON_VALUE)
@RestController
public class WorkTypesController extends BaseController {
    private final TaskManagementService taskManagementService;
    private final AccessControlService accessControlService;

    public WorkTypesController(TaskManagementService taskManagementService,
                               AccessControlService accessControlService) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
    }

    @ApiOperation("Retrieve a list of work types for the current user")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = String.class)
    })
    @GetMapping(path = "/users")
    public ResponseEntity<List<WorkType>> getWorkTypes(@RequestHeader(AUTHORIZATION) String authToken) {

        List<WorkType> workTypes = new ArrayList<>();

        AccessControlResponse roles = accessControlService.getRoles(authToken);
        if (!roles.getRoleAssignments().isEmpty()) {
            Set<String> roleWorkTypes = getActorWorkTypes(authToken, roles);

            if (!roleWorkTypes.isEmpty()) {
                for (String workTypeId : roleWorkTypes) {
                    Optional<WorkType> optionalWorkType = taskManagementService.getWorkType(workTypeId.trim());

                    optionalWorkType.ifPresent(workTypes::add);
                }
            }
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(workTypes);

    }

    private Set<String> getActorWorkTypes(String authToken, AccessControlResponse accessControlResponse) {
        Set<String> roleWorkTypes = new HashSet<>();

        String actorId = accessControlResponse.getRoleAssignments().get(0).getActorId();

        AccessControlResponse actorRoles = accessControlService.getRolesByActorId(actorId, authToken);

        for (RoleAssignment roleAssignment : actorRoles.getRoleAssignments()) {
            String assignedWorkedList = roleAssignment.getAttributes().get(WORK_TYPES.value());
            if (assignedWorkedList != null && !assignedWorkedList.isEmpty()) {
                roleWorkTypes.addAll(Arrays.asList(assignedWorkedList.split(",")));
            }
        }
        return roleWorkTypes;
    }
}
