package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getTaskIds;


@ExtendWith(MockitoExtension.class)
class DeleteTasksServiceHelperTest {

    private List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders;

    @BeforeEach
    void setUp() {
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);

        taskResourceCaseQueryBuilders = List.of(taskResourceCaseQueryBuilder1, taskResourceCaseQueryBuilder2);
    }

    @Test
    void shouldExtractTaskIds() {
        when(taskResourceCaseQueryBuilders.get(0).getTaskId()).thenReturn("123");
        when(taskResourceCaseQueryBuilders.get(1).getTaskId()).thenReturn("456");

        final List<String> taskIds = getTaskIds(taskResourceCaseQueryBuilders);

        assertThat(taskIds.get(0)).isEqualTo("123");
        assertThat(taskIds.get(1)).isEqualTo("456");
    }
}