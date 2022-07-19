package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WARNINGS;

@Slf4j
public class PostUpdateTaskWithNotesControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/notes";
    private static final String GET_TASK_ENDPOINT = "task/{task-id}";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        //Reset role assignments
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        String notesRequest = addNotes();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            notesRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));
    }

    @Test
    public void should_return_a_400_when_the_notes_are_not_provided() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            "{}",
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/bad-request"))
            .body("title", equalTo("Bad Request"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("detail", equalTo("Bad Request: Invalid request message"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_400_when_the_notes_is_empty() throws JsonProcessingException {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders());
        String notesRequest = objectMapper.writeValueAsString(new NotesRequest(emptyList()));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("violations[0].field", equalTo("note_resource"))
            .body("violations[0].message", equalTo("must not be empty"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_400_when_the_notesResource_code_is_invalid() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            addNotesWithNonValidCode(),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("violations[0].field", equalTo("code"))
            .body("violations[0].message", equalTo("must not be empty"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_400_when_the_notesResource_note_type_is_invalid() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            addNotesWithNonValidNoteType(),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("violations[0].field", equalTo("note_type"))
            .body("violations[0].message", equalTo("must not be empty"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void given_a_task_with_note_when_new_note_is_added_then_return_all_notes() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, true);

        String notesRequest = addNotes();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );
        // validate the notes
        result = restApiActions.get(
            GET_TASK_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(true));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "TA01", "warningText", "Description1"),
            Map.of("warningCode", "TA02", "warningText", "Description2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);
    }

    @Test
    public void given_a_task_when_new_note_is_added_then_return_all_notes() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, false);

        String notesRequest = addNotes();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            notesRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.NO_CONTENT.value());

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );
        // validate the notes
        result = restApiActions.get(
            GET_TASK_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
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
        return "{\"notes\": "
               + "["
               + "{"
               + "\"code\": \"TA02\","
               + "\"note_type\": \"WARNING\","
               + "\"user_id\": \"some-user\","
               + "\"content\": \"Description2\""
               + "}"
               + "]"
               + "}";
    }

    @NotNull
    private String addNotesWithNonValidCode() {
        return "{\"notes\": "
               + "["
               + "{"
               + "\"code\": \"TA02\","
               + "\"note_type\": \"WARNING\","
               + "\"user_id\": \"some-user\","
               + "\"content\": \"Description2\""
               + "},"
               + "{"
               + "\"code\": \"\","
               + "\"note_type\": \"note_type\","
               + "\"user_id\": \"\","
               + "\"content\": \"\""
               + "}"
               + "]"
               + "}";
    }

    @NotNull
    private String addNotesWithNonValidNoteType() {
        return "{\"notes\": "
               + "["
               + "{"
               + "\"code\": \"TA02\","
               + "\"note_type\": \"WARNING\","
               + "\"user_id\": \"some-user\","
               + "\"content\": \"Description2\""
               + "},"
               + "{"
               + "\"code\": \"Code\","
               + "\"note_type\": \"\","
               + "\"user_id\": \"\","
               + "\"content\": \"\""
               + "}"
               + "]"
               + "}";
    }

    private void initiateTask(TestVariables testVariables, boolean hasWarnings) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        String warnings = "[{\"warningCode\":\"TA01\", \"warningText\":\"Description1\"}]";

        InitiateTaskRequest req;
        if (hasWarnings) {
            req = new InitiateTaskRequest(
                INITIATION,
                Map.of(
                    TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                    TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                    TASK_HAS_WARNINGS.value(), true,
                    TASK_WARNINGS.value(), warnings,
                    TASK_TITLE.value(), "A test task",
                    TASK_CREATED.value(), formattedCreatedDate,
                    TASK_CASE_ID.value(), testVariables.getCaseId(),
                    TASK_DUE_DATE.value(), formattedDueDate
                )
            );
        } else {

            req = new InitiateTaskRequest(
                INITIATION,
                Map.of(
                    TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                    TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                    TASK_TITLE.value(), "A test task",
                    TASK_CREATED.value(), formattedCreatedDate,
                    TASK_CASE_ID.value(), testVariables.getCaseId(),
                    TASK_DUE_DATE.value(), formattedDueDate
                )
            );
        }

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }

}
