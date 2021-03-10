package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.VerificationData;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OperationPermissionVerifier implements Verifier<VerificationData> {

    private final CamundaObjectMapper camundaObjectMapper;

    public OperationPermissionVerifier(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    @Override
    public VerificationResult verify(final VerificationData verificationData) {

        canVerify(verificationData);
        /*
         * Optimizations: Added safe-guards to abort early as soon as a match is found
         * this saves us time and further unnecessary processing
         */
        boolean hasAccess = false;
        //comma sep list of permissions on the RoleAssignment
        Optional<String> taskRolePermissions =
            camundaObjectMapper.read(
                verificationData.getTaskVariables()
                    .get(verificationData.getRoleAssignment().getRoleName()),
                String.class
            );
        if (taskRolePermissions.isPresent()) {
            Set<String> taskPermissions = Arrays.stream(taskRolePermissions.get().split(","))
                .collect(Collectors.toSet());
            //If at least one permission is matched.
            for (PermissionTypes types : verificationData.getPermissionsRequired()) {
                //Safe-guard
                if (hasAccess) {
                    break;
                }
                hasAccess = taskPermissions.contains(types.value());

            }
        }

        return new VerificationResult(hasAccess);

    }

}
