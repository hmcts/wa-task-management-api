package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoleSignatureBuilderTest {

    @Test
    void returnEmptySignatureForEmptyFields() {
        SearchRequest searchRequest = SearchRequest.builder().build();
        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(List.of(), searchRequest);

        assertEquals(0, signature.size());
    }

    @ParameterizedTest
    @CsvSource({"ALL_WORK,PUBLIC", "ALL_WORK,PRIVATE", "ALL_WORK,RESTRICTED", "ALL_WORK,UNKNOWN",
        "AVAILABLE_TASKS,PUBLIC", "AVAILABLE_TASKS,PRIVATE", "AVAILABLE_TASKS,RESTRICTED", "AVAILABLE_TASKS,UNKNOWN",
        ",PUBLIC", ",PRIVATE", ",RESTRICTED", ",UNKNOWN"})
    void buildSignatureForPermissionRequirementAndFilterByGrantType(String requestContext, String classification) {
        RequestContext requestContextEnum = null;
        try {
            requestContextEnum = RequestContext.valueOf(requestContext);
        } catch (NullPointerException n) {
            //general context
        }

        SearchRequest searchRequest = SearchRequest.builder().requestContext(requestContextEnum).build();
        Classification classificationEnum = Classification.valueOf(classification);
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentWithStandardGrantTypeForSearchTask(classificationEnum);

        String permission = searchRequest.isAllWork() ? "m" : searchRequest.isAvailableTasksOnly() ? "a" : "r";
        String abbreviation = classificationEnum.getAbbreviation() == null ? "*" : classificationEnum.getAbbreviation();
        String expectedSignature = "IA:1:765324:hmcts-judiciary:*:"
                                   + permission
                                   + ":"
                                   + abbreviation
                                   + ":*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(1, signature.size());
        assertThat(signature, hasItem(expectedSignature));
    }

    @Test
    void buildSignatureAndFilterRoleAssignmentEmptyAttribute() {
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("WA"))
            .regions(List.of("1"))
            .locations(List.of("765324"))
            .caseIds(List.of("1623278362431003"))
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentWithoutAttributes(Classification.PUBLIC);

        String expectedSignature = "*:*:*:hmcts-judiciary:*:r:U:*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(1, signature.size());
        assertThat(signature, hasItem(expectedSignature));
    }

    @Test
    void buildSignatureAndFilterRoleAssignmentByEmptySearchRequestAttributes() {
        SearchRequest searchRequest = SearchRequest.builder()
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentWithStandardGrantTypeForSearchTask(Classification.PUBLIC);

        String expectedSignature = "IA:1:765324:hmcts-judiciary:*:r:U:*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(1, signature.size());
        assertThat(signature, hasItem(expectedSignature));
    }

    @Test
    void buildSignatureAndFilterRoleAssignmentBySearchRequest() {
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA"))
            .regions(List.of("1"))
            .locations(List.of("765324"))
            .caseIds(List.of("1623278362431003"))
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentWithDifferentAttributes(Classification.PUBLIC);

        String expectedSignature = "IA:1:765324:hmcts-judiciary:1623278362431003:r:U:*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(1, signature.size());
        assertThat(signature, hasItem(expectedSignature));
    }

    @Test
    void buildSignatureWithAuthorisationsForAvailableTaskSearch() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .jurisdictions(List.of("IA"))
            .regions(List.of("1"))
            .locations(List.of("765324"))
            .caseIds(List.of("1623278362431003"))
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentsWithCaseAndOrganisationRoleType(Classification.PUBLIC);

        String expectedSignature = "IA:1:765324:hmcts-judiciary:*:a:U:*";
        String expectedSignature2 = "IA:1:765324:hmcts-judiciary:*:a:U:Skill1";
        String expectedSignature3 = "IA:1:765324:tribunal-caseworker:1623278362431003:a:U:*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(3, signature.size());
        assertThat(signature, hasItems(expectedSignature, expectedSignature2, expectedSignature3));
    }

    @Test
    void buildSignatureWithAuthorisationsAllWorkSearch() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.ALL_WORK)
            .jurisdictions(List.of("IA"))
            .regions(List.of("1"))
            .locations(List.of("765324"))
            .caseIds(List.of("1623278362431003"))
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentsWithCaseAndOrganisationRoleType(Classification.PUBLIC);

        String expectedSignature = "IA:1:765324:hmcts-judiciary:*:m:U:*";
        String expectedSignature2 = "IA:1:765324:tribunal-caseworker:1623278362431003:m:U:*";

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(2, signature.size());
        assertThat(signature, hasItems(expectedSignature, expectedSignature2));
    }

    @Test
    void buildSignatureForMultipleRoleAssignments() {
        SearchRequest searchRequest = SearchRequest.builder()
            .jurisdictions(List.of("IA", "WA"))
            .regions(List.of("1", "2"))
            .locations(List.of("765324", "765325"))
            .caseIds(List.of("1623278362431003", "1623278362431005"))
            .build();
        List<RoleAssignment> roleAssignments = RoleAssignmentTestUtils
            .roleAssignmentWithDifferentAttributes(Classification.PUBLIC);

        Set<String> signature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        assertEquals(5, signature.size());
        assertThat(signature, hasItems("*:*:765325:tribunal-caseworker:*:r:U:*",
            "*:*:*:tribunal-caseworker:1623278362431005:r:U:*",
            "*:2:*:tribunal-caseworker:*:r:U:*",
            "WA:*:*:tribunal-caseworker:*:r:U:*",
            "IA:1:765324:hmcts-judiciary:1623278362431003:r:U:*"));
    }

}
