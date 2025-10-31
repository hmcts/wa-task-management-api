package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AwaitilityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Import(AwaitilityTestConfig.class)
@Slf4j
public class PostUpdateTaskWithNotesControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/notes";
    private static final String GET_TASK_ENDPOINT = "task/{task-id}";

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
    public void given_a_task_with_note_when_new_note_is_added_then_return_all_notes() {
        TestVariables taskVariables = common.setupWATaskWithWarningsAndRetrieveIds("processApplication",
                                                                                   "process application");
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        String notesRequest = addNotes();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        common.setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );
        // validate the notes
        result = restApiActions.get(
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
    }

    @Test
    public void given_a_task_when_new_note_is_added_then_return_all_notes() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        String notesRequest = addNotes();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        common.setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );
        // validate the notes
        result = restApiActions.get(
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
