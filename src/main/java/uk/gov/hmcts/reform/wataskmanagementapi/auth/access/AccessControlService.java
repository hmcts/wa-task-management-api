package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

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
        List<Assignment> assignments = roleAssignmentService.getRolesForUser(userInfo.getUid(), authToken);

        assignments.forEach(role -> log.debug("Response from role assignment service '{}'", role.toString()));
        return new AccessControlResponse(userInfo, assignments);
    }

    public AccessControlResponse getRolesGivenUserId(String userId, String authToken) {
        List<Assignment> assignments = roleAssignmentService.getRolesForUser(userId, authToken);
        return new AccessControlResponse(
            UserInfo.builder().uid(userId).build(),
            assignments
        );
    }
}
