package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class TaskTypeControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    TestAuthenticationCredentials caseWorkerWithWAOrgRoles;

    private static final String ENDPOINT_BEING_TESTED = "/task/task-types";

    @Before
    public void setUp() {
        caseWorkerWithWAOrgRoles = taskFunctionalTestsUserUtils
            .getTestUser(USER_WITH_WA_ORG_ROLES);
    }


    @Test
    public void should_return_task_types_for_correct_jurisdiction() {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?jurisdiction=wa",
            caseWorkerWithWAOrgRoles.getHeaders()
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

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED + "?jurisdiction=WA",
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, Map<String, String>>> actualTaskTypes = result.jsonPath().getList("task_types");

        List<Map<String, Map<String, String>>> expectedTaskTypes = getExpectedTaskTypes();

        expectedTaskTypes
            .forEach(expectedTaskType -> assertTrue(actualTaskTypes.contains(expectedTaskType)));

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
