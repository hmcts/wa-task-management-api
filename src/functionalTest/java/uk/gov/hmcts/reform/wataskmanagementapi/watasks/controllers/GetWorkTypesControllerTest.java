package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;


import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_NO_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_TRIB_CASEWORKER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class GetWorkTypesControllerTest {

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    private static final String ENDPOINT_BEING_TESTED = "work-types";

    TestAuthenticationCredentials caseWorkerWithTribRoleWithWorkTypes;
    TestAuthenticationCredentials caseWorkerWithTribRole;
    TestAuthenticationCredentials caseworkerWithNoRoles;

    @Before
    public void setUp() {
        caseworkerWithNoRoles = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_NO_ROLES);
        caseWorkerWithTribRoleWithWorkTypes = taskFunctionalTestsUserUtils
            .getTestUser(USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES);
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_TRIB_CASEWORKER_ROLE);
    }

    @Test
    public void should_return_work_types_when_user_has_work_types() {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            caseWorkerWithTribRoleWithWorkTypes.getHeaders()
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

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            caseWorkerWithTribRole.getHeaders()
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

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=false",
            caseWorkerWithTribRole.getHeaders()
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
            Map.of("id", "multi_track_decision_making_work", "label",
                   "Multi track decision making work"),
            Map.of("id", "query_work", "label", "Query work"),
            Map.of("id", "welsh_translation_work", "label", "Welsh translation work"),
            Map.of("id", "bail_work", "label", "Bail work")
        );
        Assertions.assertEquals(expectedWorkTypes, workTypes);

    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_any_roles() {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?filter-by-user=true",
            caseworkerWithNoRoles.getHeaders()
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
