package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.launchdarkly.shaded.com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@EqualsAndHashCode
public class RoleAssignmentForSearch {

    //From CCD
    //private String roleName;
    //private String jurisdiction;
    //private String caseType;
    //@EqualsAndHashCode.Exclude
    //private String caseId;
    //private String region;
    //private String location;
    //private String securityClassification;
    //@EqualsAndHashCode.Exclude
    //private RoleAssignment roleAssignment;

    //=================================//

    //private RoleCategory roleCategory;  Don't think we care for Role Assignment query.  Is used for ExUI constraints.

    @EqualsAndHashCode.Exclude
    private String id;
    @EqualsAndHashCode.Exclude
    private RoleAssignment roleAssignment; //Do we need the original RoleAssignment here?  Can probably correlate by the id

    private String roleName;
    private String classification;
    private String grantType;
    private String roleType;

    //Attributes
    private String jurisdiction;
    @EqualsAndHashCode.Exclude
    private Set<String> caseIds;
    private String caseType;
    private String region;
    private String location;

    @EqualsAndHashCode.Exclude
    private List<String> authorisations; //Do we want to group where the authorisations have the same values or skip?

    //@EqualsAndHashCode.Exclude
    //boolean isRepresentative = false;

    public RoleAssignmentForSearch(RoleAssignment roleAssignment) {
        this.id = roleAssignment.getId();
        this.roleName = roleAssignment.getRoleName();

        this.classification = roleAssignment.getClassification().name();
        this.grantType = roleAssignment.getGrantType().name();
        this.authorisations = roleAssignment.getAuthorisations();
        this.roleType = roleAssignment.getRoleType().name();

        Map<String, String> attributes = roleAssignment.getAttributes();

        //Attributes
        this.jurisdiction = attributes.get(RoleAttributeDefinition.JURISDICTION.value());
        this.caseType = attributes.get(RoleAttributeDefinition.CASE_TYPE.value());
        this.region = attributes.get(RoleAttributeDefinition.REGION.value());
        this.location = attributes.get(RoleAttributeDefinition.PRIMARY_LOCATION.value());

        this.caseIds = Sets.newHashSet();
        Optional.ofNullable(
                attributes.get(RoleAttributeDefinition.CASE_ID.value()))
            .ifPresent(s -> caseIds.add(s));


    }

}
