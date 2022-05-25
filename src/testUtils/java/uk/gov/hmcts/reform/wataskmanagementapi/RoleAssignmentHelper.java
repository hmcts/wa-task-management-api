package uk.gov.hmcts.reform.wataskmanagementapi;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;


public abstract class RoleAssignmentHelper {

    protected static final String IA_JURISDICTION = "IA";
    protected static final String IA_CASE_TYPE = "Asylum";
    protected static final String WA_JURISDICTION = "WA";
    protected static final String WA_CASE_TYPE = "WaCaseType";
    protected static final String SSCS_JURISDICTION = "SSCS";
    protected static final String PRIMARY_LOCATION = "765324";

    protected static List<RoleAssignment> createRoleAssignment(List<RoleAssignment> roleAssignments,
                                                               RoleAssignmentRequest roleAssignmentRequest) {

        Map<String, String> attributes = createAttributes(roleAssignmentRequest.getRoleAssignmentAttribute());

        RoleAssignment roleAssignment = RoleAssignment.builder()
            .roleName(roleAssignmentRequest.getTestRolesWithGrantType().getRoleName())
            .classification(roleAssignmentRequest.getTestRolesWithGrantType().getClassification())
            .grantType(roleAssignmentRequest.getTestRolesWithGrantType().getGrantType())
            .roleCategory(roleAssignmentRequest.getTestRolesWithGrantType().getRoleCategory())
            .authorisations(roleAssignmentRequest.getAuthorisations())
            .attributes(attributes)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .roleType(RoleType.CASE)
            .build();

        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }


    private static Map<String, String> createAttributes(RoleAssignmentAttribute attribute) {
        Map<String, String> attributes = new HashMap<>();

        if (nonNull(attribute.getJurisdiction())) {
            attributes.put(RoleAttributeDefinition.JURISDICTION.value(), attribute.getJurisdiction());
        }

        if (nonNull(attribute.getCaseType())) {
            attributes.put(RoleAttributeDefinition.CASE_TYPE.value(), attribute.getCaseType());
        }

        if (nonNull(attribute.getCaseId())) {
            attributes.put(RoleAttributeDefinition.CASE_ID.value(), attribute.getCaseId());
        }

        if (nonNull(attribute.getPrimaryLocation())) {
            attributes.put(RoleAttributeDefinition.PRIMARY_LOCATION.value(), attribute.getPrimaryLocation());
        }
        if (nonNull(attribute.getRegion())) {
            attributes.put(RoleAttributeDefinition.REGION.value(), attribute.getRegion());
        }
        if (nonNull(attribute.getBaseLocation())) {
            attributes.put(RoleAttributeDefinition.BASE_LOCATION.value(), attribute.getBaseLocation());
        }

        return attributes;
    }

    @Builder
    @Getter
    protected static class RoleAssignmentRequest {

        TestRolesWithGrantType testRolesWithGrantType;
        RoleAssignmentAttribute roleAssignmentAttribute;
        List<String> authorisations;

    }

    @Builder
    @Getter
    protected static class RoleAssignmentAttribute {

        String jurisdiction;
        String caseType;
        String caseId;
        String primaryLocation;
        String baseLocation;
        String region;

    }

}
