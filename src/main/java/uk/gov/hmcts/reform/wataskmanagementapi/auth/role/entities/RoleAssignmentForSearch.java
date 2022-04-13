package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.launchdarkly.shaded.com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.PRIMARY_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.REQUESTED_ROLE;

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
    private String requestedRole;

    private List<String> additionalAttributes;

    private List<String> authorisations; //if there's an authorisations list then we don't want them to be grouped together.  Keep them as they are.

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
            this.location = attributes.get(RoleAttributeDefinition.PRIMARY_LOCATION.value());
            this.requestedRole = attributes.get(REQUESTED_ROLE.value());

            Optional.ofNullable(
                    attributes.get(RoleAttributeDefinition.CASE_ID.value()))
                .ifPresent(s -> this.caseIds.add(s));

            //If there are other attributes then we want to collect these and add to additionalAttributes
            List<String> moreAttributes = attributes.keySet().stream()
                .filter(key -> Objects.nonNull(attributes.get(JURISDICTION.value())) ||
                    Objects.nonNull(attributes.get(CASE_TYPE.value())) ||
                    Objects.nonNull(attributes.get(REGION.value())) ||
                    Objects.nonNull(attributes.get(PRIMARY_LOCATION.value())) ||
                    Objects.nonNull(attributes.get(REQUESTED_ROLE.value())) ||
                    Objects.nonNull(attributes.get(CASE_ID.value())))
                .collect(Collectors.toList());

            // leave null if there are no additional ones so it does not affect the equals/hashcode algorithm
            if (!moreAttributes.isEmpty()) {
                additionalAttributes = moreAttributes;
            }
        }

    }

}
