package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

class TaskMapperTest {

    CamundaObjectMapper camundaObjectMapper;
    private TaskMapper taskMapper;

    @BeforeEach
    public void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();
        taskMapper = new TaskMapper(camundaObjectMapper);
    }

    @Test
    void should_allow_null() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));

        Task result = taskMapper.mapToTaskObject(camundaTask, variables);
        assertEquals("configured", result.getState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertNotNull(result.getCaseData());
        assertEquals("someCaseType", result.getCaseData().getCategory());
        assertEquals("someCaseName", result.getCaseData().getName());
        assertNotNull(result.getCaseData().getLocation());
        assertEquals("someStaffLocationId", result.getCaseData().getLocation().getId());
        assertEquals("someStaffLocationName", result.getCaseData().getLocation().getLocationName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee().getId());
        assertEquals("username", result.getAssignee().getUserName());
    }

    @Test
    void should_create_object_with_no_variables() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null
        );

        Task result = taskMapper.mapToTaskObject(camundaTask, new HashMap<>());
        assertNull(result.getState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertNull(result.getCaseData());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee().getId());
        assertEquals("username", result.getAssignee().getUserName());
    }

    @Test
    void should_create_object_with_case_data() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));

        Task result = taskMapper.mapToTaskObject(camundaTask, variables);
        assertEquals("configured", result.getState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertNotNull(result.getCaseData());
        assertEquals("someCaseType", result.getCaseData().getCategory());
        assertEquals("someCaseName", result.getCaseData().getName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee().getId());
        assertEquals("username", result.getAssignee().getUserName());
    }

    @Test
    void should_create_object_with_case_data_and_location() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));

        Task result = taskMapper.mapToTaskObject(camundaTask, variables);

        assertEquals("configured", result.getState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertNotNull(result.getCaseData());
        assertEquals("someCaseType", result.getCaseData().getCategory());
        assertEquals("someCaseName", result.getCaseData().getName());
        assertNotNull(result.getCaseData().getLocation());
        assertEquals("someStaffLocationId", result.getCaseData().getLocation().getId());
        assertEquals("someStaffLocationName", result.getCaseData().getLocation().getLocationName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee().getId());
        assertEquals("username", result.getAssignee().getUserName());
    }

}
