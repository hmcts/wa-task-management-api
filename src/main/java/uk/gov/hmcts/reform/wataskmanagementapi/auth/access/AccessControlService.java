package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

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
        log.info("*** email: " + userInfo.getEmail());
        log.info("*** name: " + userInfo.getName());
        log.info("*** uid: " + userInfo.getUid());
        List<Assignment> assignments = roleAssignmentService.getRolesForUser(userInfo.getUid(), authToken);

        return new AccessControlResponse(userInfo, assignments);
    }
}
