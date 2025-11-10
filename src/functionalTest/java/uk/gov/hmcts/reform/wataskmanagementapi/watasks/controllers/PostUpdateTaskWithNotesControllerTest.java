package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostUpdateTaskWithNotesControllerTest {

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/notes";
    private static final String GET_TASK_ENDPOINT = "task/{task-id}";

    @Test
    public void given_a_task_with_note_when_new_note_is_added_then_return_all_notes() {
        TestAuthenticationCredentials waCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithWarningsAndRetrieveIds("processApplication", "process application");
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String notesRequest = addNotes();

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );
        // validate the notes
        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            GET_TASK_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(true));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "Code1", "warningText", "Text1"),
            Map.of("warningCode", "Code2", "warningText", "Text2"),
            Map.of("warningCode", "TA02", "warningText", "Description2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);
        assertThat(expectedWarnings, Matchers.containsInAnyOrder(actualWarnings.toArray()));
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void given_a_task_when_new_note_is_added_then_return_all_notes() {
        TestAuthenticationCredentials waCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String notesRequest = addNotes();

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );
        // validate the notes
        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            GET_TASK_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(true));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "TA02", "warningText", "Description2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());
    }

    @NotNull
    private String addNotes() {
        return """
            {
              "notes": [
                {
                  "code": "TA02",
                  "note_type": "WARNING",
                  "user_id": "some-user",
                  "content": "Description2"
                }
              ]
            }
            """;
    }

}
