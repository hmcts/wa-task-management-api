package uk.gov.hmcts.reform.wataskmanagementapi.data;

import org.apache.groovy.util.Maps;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;

public class RoleAssignmentCreator {

    private RoleAssignmentCreator() {
        //Hidden constructor
    }

    public static RoleAssignment.RoleAssignmentBuilder aRoleAssignment() {
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

    public static RoleAssignment.RoleAssignmentBuilder aRoleAssignment(RoleType roleType,
                                                                       GrantType grantType,
                                                                       String roleName,
                                                                       Classification classification) {

        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId("actorId" + UUID.randomUUID().toString())
            .roleType(roleType)
            .roleName(roleName)
            .classification(classification)
            .grantType(grantType)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .readOnly(false)
            .beginTime(null)
            .endTime(null)
            .created(null)
            .authorisations(singletonList("731"))
            .attributes(Map.of(
                RoleAttributeDefinition.JURISDICTION.value(), "IA",
                RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
                RoleAttributeDefinition.CASE_ID.value(), "caseId:" + UUID.randomUUID().toString()
            ));

    }

    public static List<RoleAssignment> aRoleAssignmentList(int requiredSize,
                                                                       RoleType roleType,
                                                                       GrantType grantType,
                                                                       String roleName,
                                                                       Classification classification) {

        List<RoleAssignment> roles = new ArrayList<>(requiredSize);
        for (int i = 0; i < requiredSize; i++) {

            roles.add(
                aRoleAssignment(roleType, grantType, roleName, classification)
                    .build());

        }

        return roles;

    }
}
