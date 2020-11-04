package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

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

    public List<Assignment> getRoles(HttpHeaders headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getFirst(AUTHORIZATION));
        return roleAssignmentService.getRolesForUser(userInfo.getUid(), headers);
    }
}
