package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.List;

@Service
public class AccessControlService {

    private final IdamService idamService;
    private final RoleAssignmentService roleAssignmentService;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AccessControlService.class);

    @Autowired
    public AccessControlService(IdamService idamService,
                                RoleAssignmentService roleAssignmentService) {
        this.idamService = idamService;
        this.roleAssignmentService = roleAssignmentService;
    }

    public AccessControlResponse getRoles(String authToken) {
        UserInfo userInfo = idamService.getUserInfo(authToken);
        LOGGER.info("*** email: " + userInfo.getEmail());
        LOGGER.info("*** name: " + userInfo.getName());
        LOGGER.info("*** uid: " + userInfo.getUid());
        List<Assignment> assignments = roleAssignmentService.getRolesForUser(userInfo.getUid(), authToken);

        return new AccessControlResponse(userInfo, assignments);
    }
}
