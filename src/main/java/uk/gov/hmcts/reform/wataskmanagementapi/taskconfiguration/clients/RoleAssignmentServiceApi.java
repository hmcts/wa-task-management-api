package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wataskmanagementapi.config.FeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.response.RoleAssignmentResource;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "role-assignment-service-api",
    url = "${role-assignment-service.url}",
    configuration = FeignConfiguration.class
)
public interface RoleAssignmentServiceApi {

    @PostMapping(value = "/am/role-assignments/query", consumes = "application/json")
    RoleAssignmentResource queryRoleAssignments(
        @RequestHeader(AUTHORIZATION) String userToken,
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken,
        @RequestBody QueryRequest queryRequest
    );

}
