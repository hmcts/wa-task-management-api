package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.VerificationData;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;

import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PRIVATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.RESTRICTED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableKeys.SECURITY_CLASSIFICATION;

@Component
public class SecurityClassificationVerifier implements Verifier<VerificationData> {

    private final CamundaObjectMapper camundaObjectMapper;

    public SecurityClassificationVerifier(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    @Override
    public boolean canVerify(VerificationData verificationData) {
        return
            camundaObjectMapper.read(
                verificationData.getTaskVariables()
                    .get(SECURITY_CLASSIFICATION.value()),
                Classification.class
            ).isPresent();
    }

    @Override
    public VerificationResult verify(final VerificationData verificationData) {

        canVerify(verificationData);

        /*
         * If RESTRICTED on role assignment classification, then matches all levels on task
         * If PRIVATE on role assignment classification then matches "PRIVATE" and "PUBLIC" on task
         * If PUBLIC on role assignment classification then matches "PUBLIC on task.
         */

        boolean hasAccess = false;
        Optional<Classification> taskClassificationOptional =
            camundaObjectMapper.read(
                verificationData.getTaskVariables()
                    .get(SECURITY_CLASSIFICATION.value()),
                Classification.class
            );

        if (taskClassificationOptional.isPresent()) {
            Classification classification = taskClassificationOptional.get();
            switch (verificationData.getRoleAssignment().getClassification()) {
                case PUBLIC:
                    hasAccess = Objects.equals(PUBLIC, classification);
                    break;
                case PRIVATE:
                    hasAccess = asList(PUBLIC, PRIVATE).contains(classification);
                    break;
                case RESTRICTED:
                    hasAccess = asList(PUBLIC, PRIVATE, RESTRICTED).contains(classification);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected classification value");
            }
        }
        return new VerificationResult(hasAccess);

    }

}
