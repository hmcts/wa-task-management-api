package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

public interface SearchPermissionsRequired {

    List<PermissionTypes> getPermissions();

}
