package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchFilterSignatureBuilderTest {

    @Test
    void buildWildCardSignatureForEmptyFields() {
        SearchRequest searchRequest = SearchRequest.builder().build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(1, signature.size());
        assertTrue(signature.contains("*:*:*:*:*:*"));
    }

    @Test
    void buildSignatureForAllStateValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(2, signature.size());
        assertThat(signature, hasItems("A:*:*:*:*:*", "U:*:*:*:*:*"));
    }

    @Test
    void buildSignatureForAllJurisdictionValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(4, signature.size());
        assertThat(signature, hasItems("A:WA:*:*:*:*", "U:WA:*:*:*:*", "A:IA:*:*:*:*", "U:IA:*:*:*:*"));
    }

    @Test
    void buildSignatureForAllRoleCategoryValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .roleCategories(List.of(RoleCategory.ADMIN, RoleCategory.CTSC))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(8, signature.size());
        assertThat(signature, hasItems("A:WA:A:*:*:*", "U:WA:A:*:*:*", "A:IA:A:*:*:*", "U:IA:A:*:*:*",
            "A:WA:C:*:*:*", "U:WA:C:*:*:*", "A:IA:C:*:*:*", "U:IA:C:*:*:*"));
    }

    @Test
    void buildSignatureForAllWorkTypeValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .roleCategories(List.of(RoleCategory.ADMIN, RoleCategory.CTSC))
            .workTypes(List.of("evidence", "priority"))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(16, signature.size());
        assertThat(signature, hasItems(
            "A:WA:A:evidence:*:*", "U:WA:A:evidence:*:*", "A:IA:A:evidence:*:*", "U:IA:A:evidence:*:*",
            "A:WA:C:evidence:*:*", "U:WA:C:evidence:*:*", "A:IA:C:evidence:*:*", "U:IA:C:evidence:*:*",
            "A:WA:A:priority:*:*", "U:WA:A:priority:*:*", "A:IA:A:priority:*:*", "U:IA:A:priority:*:*",
            "A:WA:C:priority:*:*", "U:WA:C:priority:*:*", "A:IA:C:priority:*:*", "U:IA:C:priority:*:*"));
    }

    @Test
    void buildSignatureForAllRegionValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .roleCategories(List.of(RoleCategory.ADMIN, RoleCategory.CTSC))
            .workTypes(List.of("evidence", "priority"))
            .region(List.of("1", "2"))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(32, signature.size());
        assertThat(signature, hasItems(
            "A:WA:A:evidence:1:*", "U:WA:A:evidence:1:*", "A:IA:A:evidence:1:*", "U:IA:A:evidence:1:*",
            "A:WA:C:evidence:1:*", "U:WA:C:evidence:1:*", "A:IA:C:evidence:1:*", "U:IA:C:evidence:1:*",
            "A:WA:A:priority:1:*", "U:WA:A:priority:1:*", "A:IA:A:priority:1:*", "U:IA:A:priority:1:*",
            "A:WA:C:priority:1:*", "U:WA:C:priority:1:*", "A:IA:C:priority:1:*", "U:IA:C:priority:1:*",
            "A:WA:A:evidence:2:*", "U:WA:A:evidence:2:*", "A:IA:A:evidence:2:*", "U:IA:A:evidence:2:*",
            "A:WA:C:evidence:2:*", "U:WA:C:evidence:2:*", "A:IA:C:evidence:2:*", "U:IA:C:evidence:2:*",
            "A:WA:A:priority:2:*", "U:WA:A:priority:2:*", "A:IA:A:priority:2:*", "U:IA:A:priority:2:*",
            "A:WA:C:priority:2:*", "U:WA:C:priority:2:*", "A:IA:C:priority:2:*", "U:IA:C:priority:2:*"));
    }

    @Test
    void buildSignatureForAllLocationValues() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED))
            .jurisdictions(List.of("WA", "IA"))
            .roleCategories(List.of(RoleCategory.ADMIN, RoleCategory.CTSC))
            .workTypes(List.of("evidence", "priority"))
            .region(List.of("1", "2"))
            .locations(List.of("765324", "765325"))
            .build();
        Set<String> signature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        assertEquals(64, signature.size());
        assertThat(signature, hasItems(
            "A:WA:A:evidence:1:765324", "U:WA:A:evidence:1:765324",
            "A:IA:A:evidence:1:765324", "U:IA:A:evidence:1:765324",
            "A:WA:C:evidence:1:765324", "U:WA:C:evidence:1:765324",
            "A:IA:C:evidence:1:765324", "U:IA:C:evidence:1:765324",
            "A:WA:A:priority:1:765324", "U:WA:A:priority:1:765324",
            "A:IA:A:priority:1:765324", "U:IA:A:priority:1:765324",
            "A:WA:C:priority:1:765324", "U:WA:C:priority:1:765324",
            "A:IA:C:priority:1:765324", "U:IA:C:priority:1:765324",
            "A:WA:A:evidence:2:765324", "U:WA:A:evidence:2:765324",
            "A:IA:A:evidence:2:765324", "U:IA:A:evidence:2:765324",
            "A:WA:C:evidence:2:765324", "U:WA:C:evidence:2:765324",
            "A:IA:C:evidence:2:765324", "U:IA:C:evidence:2:765324",
            "A:WA:A:priority:2:765324", "U:WA:A:priority:2:765324",
            "A:IA:A:priority:2:765324", "U:IA:A:priority:2:765324",
            "A:WA:C:priority:2:765324", "U:WA:C:priority:2:765324",
            "A:IA:C:priority:2:765324", "U:IA:C:priority:2:765324",
            "A:WA:A:evidence:1:765325", "U:WA:A:evidence:1:765325",
            "A:IA:A:evidence:1:765325", "U:IA:A:evidence:1:765325",
            "A:WA:C:evidence:1:765325", "U:WA:C:evidence:1:765325",
            "A:IA:C:evidence:1:765325", "U:IA:C:evidence:1:765325",
            "A:WA:A:priority:1:765325", "U:WA:A:priority:1:765325",
            "A:IA:A:priority:1:765325", "U:IA:A:priority:1:765325",
            "A:WA:C:priority:1:765325", "U:WA:C:priority:1:765325",
            "A:IA:C:priority:1:765325", "U:IA:C:priority:1:765325",
            "A:WA:A:evidence:2:765325", "U:WA:A:evidence:2:765325",
            "A:IA:A:evidence:2:765325", "U:IA:A:evidence:2:765325",
            "A:WA:C:evidence:2:765325", "U:WA:C:evidence:2:765325",
            "A:IA:C:evidence:2:765325", "U:IA:C:evidence:2:765325",
            "A:WA:A:priority:2:765325", "U:WA:A:priority:2:765325",
            "A:IA:A:priority:2:765325", "U:IA:A:priority:2:765325",
            "A:WA:C:priority:2:765325", "U:WA:C:priority:2:765325",
            "A:IA:C:priority:2:765325", "U:IA:C:priority:2:765325"));
    }
}
