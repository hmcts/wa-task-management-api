package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignments;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@Service
public class AccessControlService {

    private final IdamService idamService;
    private final RoleManagementService roleManagementService;

    @Autowired
    public AccessControlService(IdamService idamService,
                                RoleManagementService roleManagementService) {
        this.idamService = idamService;
        this.roleManagementService = roleManagementService;
    }

    public RoleAssignments getRoles(HttpHeaders headers) {
        UserInfo userInfo = idamService.getUserInfo(headers.getFirst(AUTHORIZATION));
        return roleManagementService.getRolesForUser(userInfo.getUid(), headers);
    }
}
