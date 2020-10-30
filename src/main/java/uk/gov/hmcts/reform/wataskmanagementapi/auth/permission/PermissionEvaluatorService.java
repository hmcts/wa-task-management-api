package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class PermissionEvaluatorService {

    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public PermissionEvaluatorService(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public boolean hasAccess(Map<String, CamundaVariable> variables,
                             Set<String> roles,
                             List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = false;

        /*
         * Optimizations: Added safe-guards to abort early as soon as a match is found
         * this saves us time and further unnecessary processing
         */
        for (String role : roles) {

            if (hasAccess) {
                break;
            }
            String variable = getVariableValue(variables.get(role), String.class);
            if (variable != null) {
                Set<String> taskPermissions = Arrays.stream(variable.split(","))
                    .collect(Collectors.toSet());

                for (PermissionTypes p : permissionsRequired) {
                    //Safe-guard
                    if (hasAccess) {
                        break;
                    }
                    hasAccess = taskPermissions.contains(p.toString());

                }
            }
        }
        return hasAccess;
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
