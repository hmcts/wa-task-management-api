package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ReconfigureInputVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;

class TaskEntityToReconfigureInputVariableDefMapperTest {
    private final TaskEntityToReconfigureInputVariableDefMapper mapper =
        Mappers.getMapper(TaskEntityToReconfigureInputVariableDefMapper.class);
    private TaskResource taskResource;
    private ObjectMapper objectMapper = new ObjectMapper();

    private ReconfigureInputVariableDefinition reconfigureInputVarDef;

    public static final Map<String, String> EXPECTED_ADDITIONAL_PROPERTIES = Map.of(
        "name1", "value1",
        "name2", "value2"
    );

    @BeforeEach
    void setup() {
        this.objectMapper.registerModule(new JavaTimeModule());
        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
        taskResource = new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
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
            null,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(roleResource),
            "caseCategory",
            EXPECTED_ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );

    }

    @Test
    void should_map_fields_when_mapper_invoked_between_task_entity_and_reconfigure_input_variable_definition() {
        // Mapping
        reconfigureInputVarDef = mapper.map(taskResource);
        // Then
        assertThat(reconfigureInputVarDef).isNotNull();
        Map<String, Object> dbTaskAttributes =
            objectMapper.convertValue(taskResource, new TypeReference<HashMap<String, Object>>() {});
        Map<String, Object> reconfigInputAttributes =
            objectMapper.convertValue(reconfigureInputVarDef, new TypeReference<HashMap<String, Object>>() {});
        //Removed 16 attributes from reconfigInputAttributes as they are not required for reconfiguration
        assertEquals(dbTaskAttributes.size() - 17, reconfigInputAttributes.size());//Added external_task_id as part of poc work

        Set<String> dbTaskAttributeKeys = dbTaskAttributes.keySet();
        Set<String> reconfigInputAttributeKeys = reconfigInputAttributes.keySet();
        Set<String> expectedOnlyInReconfigInputKeys =
            Set.of("name", "taskState", "dueDate", "caseManagementCategory", "workType");
        //expectedOnlyInDbTaskAttributes has 19 attributes including the 4 attributes that are renamed
        reconfigInputAttributeKeys.removeAll(dbTaskAttributeKeys);
        assertThat(expectedOnlyInReconfigInputKeys).isEqualTo(reconfigInputAttributeKeys);
        reconfigInputAttributes =
            objectMapper.convertValue(reconfigureInputVarDef, new TypeReference<HashMap<String, Object>>() {});
        reconfigInputAttributeKeys = reconfigInputAttributes.keySet();
        dbTaskAttributeKeys.removeAll(reconfigInputAttributeKeys);
        Set<String> expectedOnlyInDbTaskAttributes = Set.of("lastUpdatedUser", "taskName", "dueDateTime",
                  "caseCategory", "securityClassification", "lastReconfigurationTime", "reconfigureRequestTime",
                  "autoAssigned", "state", "taskSystem", "indexed", "lastUpdatedTimestamp", "lastUpdatedAction",
                  "taskRoleResources", "executionTypeCode", "businessContext", "terminationReason", "notes",
                  "assignmentExpiry", "workTypeResource", "caseDeletionTimestamp");
        assertEquals(expectedOnlyInDbTaskAttributes, dbTaskAttributeKeys);

        assertEquals("aTaskName", reconfigureInputVarDef.getName());
        assertEquals(COMPLETED, reconfigureInputVarDef.getTaskState());
        assertEquals(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
                     reconfigureInputVarDef.getDueDate());
        assertEquals("caseCategory", reconfigureInputVarDef.getCaseManagementCategory());
        assertEquals("routine_work", reconfigureInputVarDef.getWorkType());
    }
}
