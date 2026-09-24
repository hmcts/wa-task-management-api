package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

public record TaskSearchRoleCriteria(String jurisdiction,
                                     String region,
                                     String location,
                                     String roleName,
                                     String caseId,
                                     String permission,
                                     String classification,
                                     String authorizationValue) {
}
