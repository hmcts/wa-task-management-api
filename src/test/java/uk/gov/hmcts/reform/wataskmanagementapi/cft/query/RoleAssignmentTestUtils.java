package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RoleAssignmentTestUtils {

    private RoleAssignmentTestUtils() {
        // cannot instantiate
    }

    protected static List<RoleAssignment> roleAssignmentWithSpecificGrantType(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .roleType(RoleType.CASE)
            .attributes(specificAttributes)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> roleAssignmentWithStandardGrantType(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324",
            RoleAttributeDefinition.PRIMARY_LOCATION.value(), "765324"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .roleType(RoleType.ORGANISATION)
            .classification(classification)
            .attributes(specificAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );

        roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(classification)
            .attributes(specificAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();

        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> roleAssignmentWithChallengedGrantType(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .roleType(RoleType.CASE)
            .classification(classification)
            .attributes(specificAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431004"
        );

        roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(classification)
            .attributes(specificAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();

        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> inActiveRoles() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary1")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().minusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary2")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .endTime(LocalDateTime.now().minusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary3")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary4")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().minusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary5")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> roleAssignmentWithChallengedGrantTypeAndNoAuthorizations() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PRIVATE)
            .attributes(specificAttributes)
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431004"
        );

        roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PRIVATE)
            .attributes(specificAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();

        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> roleAssignmentWithAllGrantTypes() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PRIVATE)
            .attributes(specificAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .roleType(RoleType.CASE)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(stdAttributes)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .roleType(RoleType.CASE)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> excludeddAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(excludeddAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .roleType(RoleType.CASE)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    protected static List<RoleAssignment> roleAssignmentWithoutGrantType() {

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(Classification.PUBLIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();

        return Collections.singletonList(roleAssignment);
    }

    protected static List<RoleAssignment> roleAssignmentWithSpecificGrantTypeOnly(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }
}
