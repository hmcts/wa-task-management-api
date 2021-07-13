package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;

import java.util.List;

@Slf4j
@Service
public class AccessControlService {

    private final IdamService idamService;
    private final RoleAssignmentService roleAssignmentService;

    @Autowired
    public AccessControlService(IdamService idamService,
                                RoleAssignmentService roleAssignmentService) {
        this.idamService = idamService;
        this.roleAssignmentService = roleAssignmentService;
    }

    public AccessControlResponse getRoles(String authToken) {
        UserInfo userInfo = idamService.getUserInfo(authToken);
        log.debug("UserInfo successfully retrieved from IDAM");
        List<RoleAssignment> roleAssignments = roleAssignmentService.getRolesForUser(userInfo.getUid(), authToken);

        //Safe-guard
        if (roleAssignments.isEmpty()) {
            throw new NoRoleAssignmentsFoundException(
                "User did not have sufficient permissions to perform this action"
            );
        }

        roleAssignments.forEach(role -> log.debug("Response from role assignment service '{}'", role.toString()));
        return new AccessControlResponse(userInfo, roleAssignments);
    }

    public AccessControlResponse getRolesGivenUserId(String userId, String authToken) {
        List<RoleAssignment> roleAssignments = roleAssignmentService.getRolesForUser(userId, authToken);
        return new AccessControlResponse(
            UserInfo.builder().uid(userId).build(),
            roleAssignments
        );
    }
}
