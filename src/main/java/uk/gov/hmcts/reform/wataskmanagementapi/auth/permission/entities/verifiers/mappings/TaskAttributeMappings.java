package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers.mappings;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentAttributeKeys;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableKeys;

//This is a list of Role Assignment Attributes to match against Task variables
@Component
public class TaskAttributeMappings {

    private final ImmutableMap<String, String> mappings;

    public TaskAttributeMappings() {
        mappings =
            ImmutableMap.of(
                RoleAssignmentAttributeKeys.CASE_ID.value(),
                CamundaVariableKeys.CASE_ID.value(),
                RoleAssignmentAttributeKeys.JURISDICTION.value(),
                CamundaVariableKeys.JURISDICTION.value(),
                RoleAssignmentAttributeKeys.PRIMARY_LOCATION.value(),
                CamundaVariableKeys.LOCATION.value(),
                RoleAssignmentAttributeKeys.REGION.value(),
                CamundaVariableKeys.REGION.value()
            );

    }

    public String get(String key) {
        return mappings.get(key);
    }

}

