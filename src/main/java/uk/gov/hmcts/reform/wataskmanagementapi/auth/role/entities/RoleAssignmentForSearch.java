package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.BASE_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.PRIMARY_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.REGION;

@Getter
@EqualsAndHashCode
@Slf4j
public class RoleAssignmentForSearch {

    @EqualsAndHashCode.Exclude
    private String id; //If we keep the original roleAssignment then won't need this.
    private String roleName;
    private String classification;
    private String grantType;
    private String roleType;

    //Attributes
    private String jurisdiction;
    @EqualsAndHashCode.Exclude
    private Set<String> caseIds = new HashSet<>(); //Case ids is a Set since representative caseIds will have multiple.
    private String caseType;
    private String region;
    private String location;
    private String primaryLocation;
    private String requestedRole;

    private Map<String, String> additionalAttributes;

    private List<String> authorisations; //if there's an authorisations list then we don't want them
    // to be grouped together.  Keep them as they are.

    public RoleAssignmentForSearch(RoleAssignment roleAssignment) {
        this.id = roleAssignment.getId();
        this.roleName = roleAssignment.getRoleName();
        this.classification = roleAssignment.getClassification().name();
        this.grantType = roleAssignment.getGrantType().name();
        this.authorisations = roleAssignment.getAuthorisations();
        this.roleType = roleAssignment.getRoleType().name();

        Map<String, String> attributes = roleAssignment.getAttributes();

        //Attributes
        if (!isEmpty(attributes)) {
            this.jurisdiction = attributes.get(JURISDICTION.value());
            this.caseType = attributes.get(RoleAttributeDefinition.CASE_TYPE.value());
            this.region = attributes.get(RoleAttributeDefinition.REGION.value());
            this.location = attributes.get(BASE_LOCATION.value());
            this.primaryLocation = attributes.get(PRIMARY_LOCATION.value());

            Optional.ofNullable(
                    attributes.get(RoleAttributeDefinition.CASE_ID.value()))
                .ifPresent(s -> this.caseIds.add(s));

            //If there are other attributes then we want to collect these and add to additionalAttributes

            List<String> knownAttributeKeys = List.of(JURISDICTION.value(), CASE_TYPE.value(), REGION.value(),
                                                      BASE_LOCATION.value(), PRIMARY_LOCATION.value(), CASE_ID.value()
            );
            //If there are other attributes then we want to collect these and add to additionalAttributes
            Map<String, String> moreAttributes = attributes.keySet().stream()
                .filter(key -> !knownAttributeKeys.contains(key))
                .collect(Collectors.toMap(key -> key, attributes::get));

            // leave null if there are no additional ones so it does not affect the equals/hashcode algorithm
            if (!isEmpty(moreAttributes)) {
                this.additionalAttributes = moreAttributes;
            }
        }

    }

}
