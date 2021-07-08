package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wataskmanagementapi.CftRepositoryBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDate;
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
        final Iterable<TaskResource> tasksIt = tasksRepository.findAll();

        assertTrue(StreamSupport.stream(tasksIt.spliterator(), false).count() == 1);

        final TaskResource taskResource = tasksIt.iterator().next();
        final NoteResource notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskResource.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, taskResource.getExecutionTypeResource().getExecutionCode()),
            () -> assertEquals(SecurityClassification.RESTRICTED, taskResource.getSecurityClassification()),
            () -> assertEquals(TaskState.ASSIGNED, taskResource.getState()),
            () -> assertEquals(TaskSystem.SELF, taskResource.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, taskResource.getBusinessContext()),
            () -> assertEquals(LocalDate.of(2022, 05, 9), taskResource.getAssignmentExpiry().toLocalDate()),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.getNoteType())
        );

        final Set<TaskRoleResource> taskRoles = taskResource.getTaskRoleResources();
        assertTrue(taskRoles.size() == 1);

        final TaskRoleResource taskRole = taskRoles.iterator().next();
        String[] expectedAuthorizations = {"SPECIFIC", "BASIC"};

        assertAll(
            () -> assertNotNull(taskRole.getTaskRoleId()),
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskRole.getTaskId()),
            () -> assertTrue(taskRole.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRole.getRoleName()),
            () -> assertArrayEquals(expectedAuthorizations, taskRole.getAuthorizations())
        );
    }
}
