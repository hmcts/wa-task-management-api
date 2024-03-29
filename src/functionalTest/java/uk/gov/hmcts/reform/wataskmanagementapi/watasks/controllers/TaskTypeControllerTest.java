package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskTypeControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/task-types";

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }


    @Test
    public void should_return_task_types_for_correct_jurisdiction() {

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?jurisdiction=wa",
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, Map<String, String>>> actualTaskTypes = result.jsonPath().getList("task_types");

        List<Map<String, Map<String, String>>> expectedTaskTypes = getExpectedTaskTypes();

        expectedTaskTypes.forEach(expectedTaskType -> assertTrue(actualTaskTypes.contains(expectedTaskType)));

    }

    @Test
    public void should_return_task_types_for_correct_uppercase_jurisdiction() {

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "?jurisdiction=WA",
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, Map<String, String>>> actualTaskTypes = result.jsonPath().getList("task_types");

        List<Map<String, Map<String, String>>> expectedTaskTypes = getExpectedTaskTypes();

        expectedTaskTypes.forEach(expectedTaskType -> assertTrue(actualTaskTypes.contains(expectedTaskType)));

    }

    private List<Map<String, Map<String, String>>> getExpectedTaskTypes() {

        return Lists.list(
            Map.of("task_type",
                Map.of(
                    "task_type_id", "processApplication",
                    "task_type_name", "Process Application"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "reviewAppealSkeletonArgument",
                    "task_type_name", "Review Appeal Skeleton Argument"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "decideOnTimeExtension",
                    "task_type_name", "Decide On Time Extension"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "followUpOverdueCaseBuilding",
                    "task_type_name", "Follow-up overdue case building"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "attendCma",
                    "task_type_name", "Attend Cma"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "reviewRespondentResponse",
                    "task_type_name", "Review Respondent Response"
                )
            ),
            Map.of("task_type",
                Map.of(
                    "task_type_id", "followUpOverdueRespondentEvidence",
                    "task_type_name", "Follow-up overdue respondent evidence"
                )
            )
        );
    }

}
