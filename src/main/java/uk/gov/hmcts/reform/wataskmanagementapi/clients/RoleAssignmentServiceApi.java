package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamelCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.MultipleQueryRequest;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "role-assignment-api",
    url = "${role-assignment-service.url}",
    configuration = CamelCaseFeignConfiguration.class
)
@SuppressWarnings("checkstyle:LineLength")
public interface RoleAssignmentServiceApi {

    String V2_MEDIA_TYPE_POST_ASSIGNMENTS =
        "application/vnd.uk.gov.hmcts.role-assignment-service"
        + ".post-assignment-query-request+json;charset=UTF-8;version=2.0";

    @GetMapping(
        value = "/am/role-assignments/actors/{user-id}",
        produces = "application/vnd.uk.gov.hmcts.role-assignment-service.get-assignments+json;charset=UTF-8;version=1.0"
    )
    RoleAssignmentResource getRolesForUser(@PathVariable("user-id") String userId,
                                           @RequestHeader(AUTHORIZATION) String userToken,
                                           @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken);

    @DeleteMapping(
        value = "/am/role-assignments/{role-assignment-id}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void deleteRoleAssignmentById(@PathVariable("role-assignment-id") String roleAssignmentId,
                                  @RequestHeader(AUTHORIZATION) String userToken,
                                  @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken);

    @PostMapping(
        value = "/am/role-assignments",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void createRoleAssignment(@RequestBody String body,
                              @RequestHeader(AUTHORIZATION) String userToken,
                              @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken);

    @PostMapping(
        value = "/am/role-assignments/query",
        consumes = V2_MEDIA_TYPE_POST_ASSIGNMENTS,
        produces = V2_MEDIA_TYPE_POST_ASSIGNMENTS)
    RoleAssignmentResource queryRoleAssignments(
        @RequestHeader(AUTHORIZATION) String userToken,
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken,
        @RequestBody MultipleQueryRequest queryRequest
    );
}
