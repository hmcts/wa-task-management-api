package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class CaseRoleAssignmentForSearch {

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

    //Attributes
    private String jurisdiction;
    @EqualsAndHashCode.Exclude
    private List<String> caseIds;
    private String caseType;
    private String region;
    private String location;

    @EqualsAndHashCode.Exclude
    private List<String> authorisations; //Do we want to group where the authorisations have the same values or skip?

    //@EqualsAndHashCode.Exclude
    //boolean isRepresentative = false;

    public CaseRoleAssignmentForSearch(RoleAssignment roleAssignment) {

    }
}
