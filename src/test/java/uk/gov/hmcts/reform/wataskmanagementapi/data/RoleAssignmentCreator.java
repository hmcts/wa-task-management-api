package uk.gov.hmcts.reform.wataskmanagementapi.data;

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
        return aRoleAssignment("acaseid", "tribunal-caseworker", RoleType.ORGANISATION);
    }

    public static RoleAssignment.RoleAssignmentBuilder aRoleAssignment(String caseId, String roleName,
                                                                       RoleType roleType) {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId("someActorId")
            .roleType(roleType)
            .roleName(roleName)
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
                RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
                RoleAttributeDefinition.CASE_ID.value(), caseId
            ));
    }

    public static RoleAssignment.RoleAssignmentBuilder aRoleAssignment(RoleType roleType,
                                                                       GrantType grantType,
                                                                       String roleName,
                                                                       Classification classification,
                                                                       List<String> authorisations) {

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
            .authorisations(authorisations)
            .attributes(getAttributes(roleName, roleType.name()));
    }

    private static Map<String, String> getAttributes(String roleName, String roleType) {
        Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.CASE_ID.value(), "caseId:" + UUID.randomUUID()
        );
        Map<String, String> caseworkerAttributes = Map.of(RoleAttributeDefinition.JURISDICTION.value(), "IA",
                                                          RoleAttributeDefinition.PRIMARY_LOCATION.value(), "123456",
                                                          RoleAttributeDefinition.BASE_LOCATION.value(), "123456"
        );

        switch (roleName) {
            case "hearing-judge":
            case "case-manager":
                return specificAttributes;
            case "task-supervisor":
                return Map.of(RoleAttributeDefinition.JURISDICTION.value(), "IA");

            case "case-allocator":
            case "tribunal-caseworker":
                return roleType.equals("CASE")
                    ? specificAttributes
                    : caseworkerAttributes;
            case "hearing-centre-admin":
            case "senior-tribunal-caseworker":
                return caseworkerAttributes;
            case "hmcts-admin":
            case "hmcts-legal-operations":
                return Map.of(RoleAttributeDefinition.PRIMARY_LOCATION.value(), "123456");
            case "specific-access-requested":
                return Map.of(
                    RoleAttributeDefinition.JURISDICTION.value(), "IA",
                    RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
                    RoleAttributeDefinition.CASE_ID.value(), "caseId:" + UUID.randomUUID(),
                    RoleAttributeDefinition.REQUESTED_ROLE.value(), "requested-role"
                );

            case "additional-attributes-requested":
                return Map.of(
                    RoleAttributeDefinition.JURISDICTION.value(), "IA",
                    RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
                    RoleAttributeDefinition.CASE_ID.value(), "caseId:" + UUID.randomUUID(),
                    RoleAttributeDefinition.REQUESTED_ROLE.value(), "requested-role",
                    "ADDITIONAL_ATTRIBUTE", "additional-attribute"
                );
            default:
                throw new IllegalArgumentException("unknown role type");
        }
    }

    public static List<RoleAssignment> aRoleAssignmentList(int requiredSize,
                                                           RoleType roleType,
                                                           GrantType grantType,
                                                           String roleName,
                                                           Classification classification,
                                                           List<String> authorisations) {

        List<RoleAssignment> roles = new ArrayList<>(requiredSize);
        for (int i = 0; i < requiredSize; i++) {
            roles.add(aRoleAssignment(roleType, grantType, roleName, classification, authorisations).build());
        }

        return roles;
    }
}
