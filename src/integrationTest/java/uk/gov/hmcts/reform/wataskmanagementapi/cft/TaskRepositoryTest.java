package uk.gov.hmcts.reform.wataskmanagementapi.cft;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wataskmanagementapi.CftRepositoryBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoles;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.Tasks;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TasksRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.utils.Notes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskRepositoryTest extends CftRepositoryBaseTest {

    @Autowired
    private TasksRepository tasksRepository;

    @Test
    @Sql("/scripts/data.sql")
    void shouldReadTaskData() {
        final Iterable<Tasks> tasksIt = tasksRepository.findAll();

        assertTrue(StreamSupport.stream(tasksIt.spliterator(), false).count() == 1);

        final Tasks tasks = tasksIt.iterator().next();
        final Notes notes = tasks.getNotes();

        assertAll(
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", tasks.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, tasks.getExecutionTypeCode().getExecutionCode()),
            () -> assertEquals(SecurityClassification.RESTRICTED, tasks.getSecurityClassification()),
            () -> assertEquals(TaskState.ASSIGNED, tasks.getState()),
            () -> assertEquals(TaskSystem.SELF, tasks.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, tasks.getBusinessContext()),
            () -> assertEquals(LocalDate.of(2022, 05, 9), tasks.getAssignmentExpiry().toLocalDate()),
            () -> assertEquals(ZoneOffset.of("+01:00"), tasks.getAssignmentExpiry().getOffset()),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.getNoteType())
        );

        final Set<TaskRoles> taskRolesSet = tasks.getTaskRoles();
        assertTrue(taskRolesSet.size() == 1);

        final TaskRoles taskRoles = taskRolesSet.iterator().next();
        String[] expectedAuthorisations = {"SPECIFIC", "BASIC"};

        assertAll(
            () -> assertNotNull(taskRoles.getTaskRoleId()),
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskRoles.getTaskId()),
            () -> assertTrue(taskRoles.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRoles.getRoleName()),
            () -> assertArrayEquals(expectedAuthorisations, taskRoles.getAuthorisations())
        );
    }
}
