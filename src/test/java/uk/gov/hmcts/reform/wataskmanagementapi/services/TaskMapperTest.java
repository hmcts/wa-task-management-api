package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

class TaskMapperTest {
    public static final Map<String, String> EXPECTED_ADDITIONAL_PROPERTIES = Map.of(
        "name1",
        "value1",
        "name2",
        "value2"
    );

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
            null,
            "some-key",
            "someProcessInstanceId"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseTypeId", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("someClassification", "String"));
        variables.put("workType", new CamundaVariable("someWorkType", "String"));

        Task result = taskMapper.mapToTaskObject(variables, camundaTask);
        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseName", result.getCaseName());
        assertNotNull(result.getLocation());
        assertEquals("someStaffLocationId", result.getLocation());
        assertEquals("someStaffLocationName", result.getLocationName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee());
        assertEquals("someCaseTypeId", result.getCaseTypeId());
        assertEquals("someClassification", result.getSecurityClassification());
        assertEquals("someWorkType", result.getWorkTypeId());
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
            null,
            "some-key",
            "someProcessInstanceId"
        );

        Task result = taskMapper.mapToTaskObject(new HashMap<String, CamundaVariable>(), camundaTask);
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee());
        assertEquals("someTaskName", result.getName());

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
            null,
            "some-key",
            "someProcessInstanceId"
        );
        WarningValues warningValues = new WarningValues(
            Arrays.asList(
                new Warning("123", "some warning"),
                new Warning("456", "some more warning")
            ));
        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("appealType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("securityClassification", new CamundaVariable("someClassification", "String"));
        variables.put("hasWarnings", new CamundaVariable(false, "Boolean"));
        variables.put("warningList", new CamundaVariable(warningValues, "WarningValues"));
        variables.put("caseManagementCategory", new CamundaVariable("someCaseManagementCategory", "String"));


        Task result = taskMapper.mapToTaskObject(variables, camundaTask);
        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseCategory());
        assertEquals("someTaskName", result.getName());
        assertNotNull(result.getAssignee());
        assertEquals("someAssignee", result.getAssignee());
        assertEquals("someCaseName", result.getCaseName());
        assertEquals("someClassification", result.getSecurityClassification());
        assertEquals(false, result.getWarnings());
        assertNotNull(result.getWarningList());
        assertEquals("someCaseManagementCategory", result.getCaseManagementCategory());
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
            "some-key",
            "someProcessInstanceId"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("someClassification", "String"));

        Task result = taskMapper.mapToTaskObject(variables, camundaTask);

        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseTypeId());
        assertEquals("someCaseName", result.getCaseName());
        assertNotNull(result.getLocation());
        assertEquals("someStaffLocationId", result.getLocation());
        assertEquals("someStaffLocationName", result.getLocationName());
        assertEquals("someClassification", result.getSecurityClassification());

        assertNotNull(result.getAssignee());
    }

    @Test
    void should_map_additional_properties() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        CamundaTask camundaTask = new CamundaTask(
            "someId",
            "someTaskName",
            "someAssignee",
            ZonedDateTime.now(),
            dueDate,
            null,
            null,
            "some-key",
            "someProcessInstanceId"
        );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("someClassification", "String"));
        variables.put(
            "additionalProperties",
            new CamundaVariable(EXPECTED_ADDITIONAL_PROPERTIES, "String")
        );

        Task result = taskMapper.mapToTaskObject(variables, camundaTask);

        assertEquals("configured", result.getTaskState());
        assertEquals(dueDate, result.getDueDate());
        assertEquals("someTaskName", result.getName());
        assertEquals("someCaseType", result.getCaseTypeId());
        assertEquals("someCaseName", result.getCaseName());
        assertNotNull(result.getLocation());
        assertEquals("someStaffLocationId", result.getLocation());
        assertEquals("someStaffLocationName", result.getLocationName());
        assertEquals("someClassification", result.getSecurityClassification());
        assertEquals(EXPECTED_ADDITIONAL_PROPERTIES, result.getAdditionalProperties());

        assertNotNull(result.getAssignee());
    }
}
