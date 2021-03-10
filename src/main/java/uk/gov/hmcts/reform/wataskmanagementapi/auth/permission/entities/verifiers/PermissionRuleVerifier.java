package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.VerificationData;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers.mappings.TaskAttributeMappings;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
public class PermissionRuleVerifier implements Verifier<VerificationData> {

    private final CamundaObjectMapper camundaObjectMapper;
    private final TaskAttributeMappings taskAttributeMappings;

    public PermissionRuleVerifier(CamundaObjectMapper camundaObjectMapper,
                                  TaskAttributeMappings taskAttributeMappings) {
        this.camundaObjectMapper = camundaObjectMapper;
        this.taskAttributeMappings = taskAttributeMappings;

    }

    @Override
    public boolean canVerify(VerificationData verificationData) {
        requireNonNull(
            verificationData.getRoleAssignment().getAttributes(),
            "Role Assignment Attributes cannot be null."
        );

        return true;
    }

    //Verify if the Task variable matches with the Role Assignment variable
    @Override
    public VerificationResult verify(VerificationData verificationData) {

        canVerify(verificationData);

        //This should loop through a list of available permission rules.
        final Map<String, String> attributes =
            verificationData.getRoleAssignment().getAttributes();

        Optional<Boolean> failuresFound = attributes.keySet()
            .stream()
            .map(attribute -> {

                //Get Task key from mappings
                String taskAttrKey =
                    Optional.of(taskAttributeMappings.get(attribute))
                        .orElse("noTaskAttributeKeyFound");

                //Get Task var using key
                String taskVariable = camundaObjectMapper.read(
                    verificationData.getTaskVariables()
                        .get(taskAttrKey), String.class)
                    .orElse("noTaskVariableFound");

                //test if match found
                return matchRule(attribute, taskVariable);
            })
            .filter(doAttributeAndTaskVariableMatch -> !doAttributeAndTaskVariableMatch)
            .findAny();

        return new VerificationResult(failuresFound.isEmpty());

    }

    public boolean matchRule(String roleAssigmentAttribute, String taskVariable) {
        return roleAssigmentAttribute.equals(taskVariable);
    }

}
