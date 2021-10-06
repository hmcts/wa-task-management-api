package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;

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
    private final CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    private final AccessControlService accessControlService;

    public WorkTypesController(CFTWorkTypeDatabaseService cftWorkTypeDatabaseService,
                               AccessControlService accessControlService) {
        super();
        this.cftWorkTypeDatabaseService = cftWorkTypeDatabaseService;
        this.accessControlService = accessControlService;
    }

    @ApiOperation("Retrieve a list of work types with or without filter by user")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetTaskResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @GetMapping
    public ResponseEntity<GetWorkTypesResponse<WorkType>> getWorkTypes(
        @RequestHeader(AUTHORIZATION) String authToken,
        @RequestParam(
            required = false, name = "filter-by-user", defaultValue = "false") boolean filterByUser
    ) {
        AccessControlResponse roles = accessControlService.getRoles(authToken);
        List<WorkType> workTypes = new ArrayList<>();
        if (filterByUser) {
            if (!roles.getRoleAssignments().isEmpty()) {
                Set<String> roleWorkTypes = getActorWorkTypes(roles);

                if (!roleWorkTypes.isEmpty()) {
                    for (String workTypeId : roleWorkTypes) {
                        Optional<WorkType> optionalWorkType = cftWorkTypeDatabaseService.getWorkType(workTypeId.trim());

                        optionalWorkType.ifPresent(workTypes::add);
                    }
                }
            }
        } else {
            workTypes = cftWorkTypeDatabaseService.getAllWorkTypes();
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetWorkTypesResponse<>(workTypes));
    }

    private Set<String> getActorWorkTypes(AccessControlResponse accessControlResponse) {
        Set<String> roleWorkTypes = new HashSet<>();

        for (RoleAssignment roleAssignment : accessControlResponse.getRoleAssignments()) {
            String assignedWorkedList = roleAssignment.getAttributes().get(WORK_TYPES.value());
            if (assignedWorkedList != null && !assignedWorkedList.isEmpty()) {
                roleWorkTypes.addAll(Arrays.asList(assignedWorkedList.split(",")));
            }
        }
        return roleWorkTypes;
    }
}
