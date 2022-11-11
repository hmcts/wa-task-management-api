package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class TestAssertionsBuilder {

    private TestAssertionsBuilder() {
    }

    public static Map<String, Matcher<?>> buildTaskActionAttributesForAssertion(String taskId,
                                                                                String assigneeId,
                                                                                String taskState,
                                                                                String lastUpdatedUserId,
                                                                                TaskAction taskAction) {

        Matcher<?> assigneeMatcher = Optional.ofNullable(assigneeId)
            .map(id -> equalTo((Object) assigneeId))
            .orElseGet(() -> nullValue());

        return Map.of(
            "task.id", equalTo(taskId),
            "task.task_state", CoreMatchers.is(taskState),
            "task.assignee", assigneeMatcher,
            "task.last_updated_timestamp", notNullValue(),
            "task.last_updated_user", equalTo(lastUpdatedUserId),
            "task.last_updated_action", equalTo(taskAction.getValue())
        );

    }

}
