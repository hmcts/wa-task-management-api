package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.WORK_TYPES;

@Slf4j
@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class WorkTypesService {

    private final CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;

    @Autowired
    public WorkTypesService(CFTWorkTypeDatabaseService cftWorkTypeDatabaseService) {
        this.cftWorkTypeDatabaseService = cftWorkTypeDatabaseService;
    }

    /**
     * Retrieves a work type for a user given role assignments.
     *
     * @param accessControlResponse containing the access management roles.
     * @return A mapped optional of work type {@link WorkType}
     */
    public List<WorkType> getWorkTypes(AccessControlResponse accessControlResponse) {

        //Safe-guard
        if (accessControlResponse.getRoleAssignments().isEmpty()) {
            return emptyList();
        }

        Set<String> actorWorkTypes = extractActorWorkTypes(accessControlResponse);

        //Safe-guard
        if (actorWorkTypes.isEmpty()) {
            return emptyList();
        }
        return cftWorkTypeDatabaseService.getWorkTypes(actorWorkTypes);

    }

    public List<WorkType> getAllWorkTypes() {
        return cftWorkTypeDatabaseService.getAllWorkTypes();
    }

    private Set<String> extractActorWorkTypes(AccessControlResponse accessControlResponse) {
        Set<String> roleWorkTypes = new HashSet<>();

        for (RoleAssignment roleAssignment : accessControlResponse.getRoleAssignments()) {
            String assignedWorkedList = roleAssignment.getAttributes().get(WORK_TYPES.value());
            if (assignedWorkedList != null) {
                roleWorkTypes.addAll(asList(assignedWorkedList.split(",")));
            }
        }
        return roleWorkTypes;
    }
}
