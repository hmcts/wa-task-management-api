package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;


import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class GetWorkTypesControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "work-types";

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R2);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_work_types_when_user_has_work_types() {
        common.setupWAOrganisationalRoleAssignmentWithWorkTypes(waCaseworkerCredentials.getHeaders(),
            "tribunal-caseworker");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            waCaseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("id", "hearing_work", "label", "Hearing work"),
            Map.of("id", "upper_tribunal", "label", "Upper Tribunal"),
            Map.of("id", "routine_work", "label", "Routine work")
        );
        Assertions.assertEquals(expectedWarnings, workTypes);

    }

    @Test
    public void should_return_empty_work_types_when_user_has_no_work_types() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "tribunal-caseworker");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            waCaseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        Assertions.assertTrue(workTypes.isEmpty());

    }

    @Test
    public void should_return_all_work_types() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "tribunal-caseworker");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=false",
            waCaseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        List<Map<String, String>> expectedWorkTypes = Lists.list(
            Map.of("id", "hearing_work", "label", "Hearing work"),
            Map.of("id", "upper_tribunal", "label", "Upper Tribunal"),
            Map.of("id", "routine_work", "label", "Routine work"),
            Map.of("id", "decision_making_work", "label", "Decision-making work"),
            Map.of("id", "applications", "label", "Applications"),
            Map.of("id", "priority", "label", "Priority"),
            Map.of("id", "access_requests", "label", "Access requests"),
            Map.of("id", "error_management", "label", "Error management"),
            Map.of("id", "review_case", "label", "Review Case"),
            Map.of("id", "evidence", "label", "Evidence"),
            Map.of("id", "follow_up", "label", "Follow Up"),
            Map.of("id", "pre_hearing", "label", "Pre-Hearing"),
            Map.of("id", "post_hearing", "label", "Post-Hearing"),
            Map.of("id", "intermediate_track_hearing_work", "label", "Intermediate track hearing work"),
            Map.of("id", "multi_track_hearing_work", "label", "Multi track hearing work"),
            Map.of("id", "intermediate_track_decision_making_work",
                   "label", "Intermediate track decision making work"),
            Map.of("id", "multi_track_decision_making_work", "label", "Multi track decision making work"),
            Map.of("id", "query_work", "label", "Query work"),
            Map.of("id", "welsh_translation_work", "label", "Welsh translation work"),
            Map.of("id", "bail_work", "label", "Bail work")
        );
        Assertions.assertEquals(expectedWorkTypes, workTypes);

    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_any_roles() {

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()));
    }
}
