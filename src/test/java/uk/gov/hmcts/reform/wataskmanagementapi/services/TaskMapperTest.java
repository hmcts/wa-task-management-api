package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.CreateHmctsTaskVariable;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

class TaskMapperTest {

    CamundaObjectMapper camundaObjectMapper;

    private CreateHmctsTaskVariable taskMapper;

    @BeforeEach
    public void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();
        taskMapper = new CreateHmctsTaskVariable(camundaObjectMapper);
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
            null,
            "some-key"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));

        Task result = taskMapper.mapToTaskObject(variables, camundaTask);
        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseTypeId());
        assertEquals("someCaseName", result.getName());
        assertNotNull(result.getLocation());
        assertEquals("someStaffLocationId", result.getLocation());
        assertEquals("someStaffLocationName", result.getLocationName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee());
        assertEquals("username", result.getName());
    }

    //@Test
    //void should_create_object_with_no_variables() {
    //
    //    ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
    //
    //    CamundaTask camundaTask = new CamundaTask(
    //        "someId",
    //        "someTaskName",
    //        "someAssignee",
    //        ZonedDateTime.now(),
    //        dueDate,
    //        null,
    //        null,
    //        "some-key"
    //    );
    //
    //    Task result = taskMapper.mapToTaskObject(camundaTask, new HashMap<String, CamundaVariable>());
    //    assertEquals(dueDate, result.getDueDate());
    //    assertEquals("someTaskName", result.getName());
    //    assertNotNull(result.getAssignee());
    //    assertEquals("someAssignee", result.getAssignee());
    //    assertEquals("username", result.getName());
    //}

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
            null,
            "some-key"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));

        Task result = taskMapper.mapToTaskObject(variables,camundaTask);
        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseCategory());
        assertEquals("someCaseName", result.getName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee());
        assertEquals("username", result.getName());
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
            null,
            "some-key"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));

        Task result = taskMapper.mapToTaskObject(variables, camundaTask);

        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseCategory());
        assertEquals("someCaseName", result.getName());
        assertNotNull(result.getLocation());
        assertEquals("someStaffLocationId", result.getLocation());
        assertEquals("someStaffLocationName", result.getLocationName());
        assertNotNull(result.getAssignee());
    }

}
