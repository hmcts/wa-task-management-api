package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.CftRepositoryBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskResourceRepositoryTest extends CftRepositoryBaseTest {

    @Autowired
    private TaskResourceRepository taskResourceRepository;


    @BeforeEach
    void setUp() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void shouldReadTaskData() {
        TaskResource str = createAndSaveTask();
        assertEquals(1, taskResourceRepository.count());

        assertTrue(taskResourceRepository.findById(str.getTaskId()).isPresent());
        WorkTypeResource workTypeResource = taskResourceRepository
            .findById(str.getTaskId()).get().getWorkTypeResource();
        assertEquals("routine_work", workTypeResource.getId());
        assertEquals("Routine work", workTypeResource.getLabel());

        final Iterable<TaskResource> tasksIt = taskResourceRepository.findAll();

        final TaskResource taskResource = tasksIt.iterator().next();
        final List<NoteResource> notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskResource.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, taskResource.getExecutionTypeCode().getExecutionCode()),
            () -> assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification()),
            () -> assertEquals(CFTTaskState.COMPLETED, taskResource.getState()),
            () -> assertEquals(TaskSystem.SELF, taskResource.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, taskResource.getBusinessContext()),
            () -> assertEquals(LocalDate.of(2022, 05, 9), taskResource.getAssignmentExpiry().toLocalDate()),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.get(0).getNoteType())
        );

        final Set<TaskRoleResource> taskRoles = taskResource.getTaskRoleResources();
        assertEquals(1, taskRoles.size());

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

    private TaskResource createAndSaveTask() {
        List<NoteResource> notes = singletonList(
            new NoteResource("someCode",
                             "noteTypeVal",
                             "userVal", OffsetDateTime.now(),
                             "someContent"
            ));
        TaskResource taskResource = new TaskResource(
            "8d6cc5cf-c973-11eb-bdba-0242ac11001e",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            notes,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(new TaskRoleResource(
                "tribunal-caseofficer",
                true,
                false,
                false,
                false,
                false,
                false,
                new String[]{"SPECIFIC", "BASIC"},
                0,
                false,
                "JUDICIAL",
                "8d6cc5cf-c973-11eb-bdba-0242ac11001e",
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
            ))
        );

        return taskResourceRepository.save(taskResource);

    }
}
