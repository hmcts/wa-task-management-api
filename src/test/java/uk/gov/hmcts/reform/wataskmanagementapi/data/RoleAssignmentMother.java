package uk.gov.hmcts.reform.wataskmanagementapi.data;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;

public class RoleAssignmentMother {
    private RoleAssignmentMother() {
        //Hidden constructor
    }

    public static RoleAssignment.RoleAssignmentBuilder complete() {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId("someActorId")
            .roleType(RoleType.ORGANISATION)
            .roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .readOnly(false)
            .beginTime(null)
            .endTime(null)
            .created(null)
            .authorisations(singletonList("731"))
            .attributes(Map.of(
                RoleAttributeDefinition.JURISDICTION.value(), "IA",
                RoleAttributeDefinition.CASE_TYPE.value(), "Asylum"
            ));
    }
}
