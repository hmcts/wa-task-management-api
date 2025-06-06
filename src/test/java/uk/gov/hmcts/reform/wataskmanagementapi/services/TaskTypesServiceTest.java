package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTypesServiceTest {

    @Mock
    private DmnEvaluationService dmnEvaluationService;
    private TaskTypesService taskTypesService;

    @Test
    void should_return_task_types() {
        //given
        TaskTypesDmnResponse taskTypesDmnResponse = new TaskTypesDmnResponse(
            "wa-task-types-wa-wacasetype",
            "wa",
            "wa-task-types-wa-wacasetype.dmn"
        );
        Set<TaskTypesDmnResponse> taskTypesDmnResponses = Set.of(taskTypesDmnResponse);

        when(dmnEvaluationService.retrieveTaskTypesDmn("wa", "wa-task-types-"))
            .thenReturn(taskTypesDmnResponses);


        CamundaValue<String> taskTypeId = new CamundaValue<>("processApplication", "String");
        CamundaValue<String> taskTypeName = new CamundaValue<>("Process Application", "String");

        TaskTypesDmnEvaluationResponse taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
            taskTypeId, taskTypeName
        );

        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = List.of(taskTypesDmnEvaluationResponse);
        when(dmnEvaluationService.evaluateTaskTypesDmn("wa", "wa-task-types-wa-wacasetype"))
            .thenReturn(taskTypesDmnEvaluationResponses);

        final List<String> roleNames = singletonList("tribunal-caseworker");
        Map<String, String> roleAttributes = new HashMap<>();
        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);

        //when
        GetTaskTypesResponse response = taskTypesService.getTaskTypes(accessControlResponse, "wa");

        //then
        assertNotNull(response);
        assertNotNull(response.getTaskTypeResponses());
        assertEquals(1, response.getTaskTypeResponses().size());
        assertEquals("processApplication", response.getTaskTypeResponses()
                .stream().toList().get(0).getTaskType().getTaskTypeId());
        assertEquals("Process Application", response.getTaskTypeResponses()
                .stream().toList().get(0).getTaskType().getTaskTypeName());

    }

    @Test
    void should_return_all_task_types_with_first_record_and_without_duplicate() {

        //given
        TaskTypesDmnResponse taskTypesDmnResponse = new TaskTypesDmnResponse(
                "wa-task-types-wa-wacasetype",
                "wa",
                "wa-task-types-wa-wacasetype.dmn"
        );
        Set<TaskTypesDmnResponse> taskTypesDmnResponses = Set.of(taskTypesDmnResponse);

        when(dmnEvaluationService.retrieveTaskTypesDmn("wa", "wa-task-types-"))
                .thenReturn(taskTypesDmnResponses);


        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = new ArrayList<>();
        //first record
        CamundaValue<String> taskTypeId = new CamundaValue<>("processApplication", "String");
        CamundaValue<String> taskTypeName = new CamundaValue<>("Process Application", "String");

        TaskTypesDmnEvaluationResponse taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
                taskTypeId, taskTypeName
        );
        taskTypesDmnEvaluationResponses.add(taskTypesDmnEvaluationResponse);

        //second record
        taskTypeId = new CamundaValue<>("reviewAppealSkeletonArgument", "String");
        taskTypeName = new CamundaValue<>("Review Appeal Skeleton Argument", "String");

        taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
                taskTypeId, taskTypeName
        );
        taskTypesDmnEvaluationResponses.add(taskTypesDmnEvaluationResponse);

        //third record (duplicate-with different taskTypeName)
        taskTypeId = new CamundaValue<>("processApplication", "String");
        taskTypeName = new CamundaValue<>("Process Application-2", "String");

        taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
                taskTypeId, taskTypeName
        );
        taskTypesDmnEvaluationResponses.add(taskTypesDmnEvaluationResponse);

        //fourth record (duplicate)
        taskTypeId = new CamundaValue<>("processApplication", "String");
        taskTypeName = new CamundaValue<>("Process Application", "String");

        taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
                taskTypeId, taskTypeName
        );
        taskTypesDmnEvaluationResponses.add(taskTypesDmnEvaluationResponse);

        //fifth record (duplicate-with upperCase taskTypeId)
        taskTypeId = new CamundaValue<>("PROCESSAPPLICATION", "String");
        taskTypeName = new CamundaValue<>("Process Application", "String");

        taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(
                taskTypeId, taskTypeName
        );
        taskTypesDmnEvaluationResponses.add(taskTypesDmnEvaluationResponse);

        when(dmnEvaluationService.evaluateTaskTypesDmn("wa", "wa-task-types-wa-wacasetype"))
                .thenReturn(taskTypesDmnEvaluationResponses);

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        List<RoleAssignment> allTestRoles = createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, allTestRoles);

        //when
        GetTaskTypesResponse response = taskTypesService.getTaskTypes(accessControlResponse, "wa");

        //then
        assertNotNull(response);
        assertNotNull(response.getTaskTypeResponses());
        assertEquals(2, response.getTaskTypeResponses().size());
        assertEquals(
                "processApplication",
                response.getTaskTypeResponses()
                    .stream().toList().get(0).getTaskType().getTaskTypeId()
        );
        assertEquals(
                "Process Application",
            response.getTaskTypeResponses()
                .stream().toList().get(0).getTaskType().getTaskTypeName()
        );
        assertEquals(
                "reviewAppealSkeletonArgument",
            response.getTaskTypeResponses()
                .stream().toList().get(1).getTaskType().getTaskTypeId()
        );
        assertEquals(
                "Review Appeal Skeleton Argument",
            response.getTaskTypeResponses()
                .stream().toList().get(1).getTaskType().getTaskTypeName()
        );
    }

    @Test
    void should_return_empty_list_when_role_assignments_empty() {
        //given
        List<RoleAssignment> roleAssignments = emptyList();
        AccessControlResponse accessControlResponse = new AccessControlResponse(null, roleAssignments);

        //when
        GetTaskTypesResponse response = taskTypesService.getTaskTypes(accessControlResponse, "wa");

        //then
        assertNotNull(response);
        assertNull(response.getTaskTypeResponses());
    }

    @BeforeEach
    void setUp() {
        taskTypesService = new TaskTypesService(
            dmnEvaluationService
        );
    }

    private List<RoleAssignment> createTestRoleAssignmentsWithRoleAttributes(List<String> roleNames,
                                                                             Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
                .forEach(roleType -> {
                    RoleAssignment roleAssignment = createBaseAssignment(
                            UUID.randomUUID().toString(),
                            "tribunal-caseworker",
                            roleType,
                            Classification.PUBLIC,
                            roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }));
        return allTestRoles;
    }

    private RoleAssignment createBaseAssignment(String actorId,
                                                String roleName,
                                                RoleType roleType,
                                                Classification classification,
                                                Map<String, String> attributes) {
        return new RoleAssignment(
                ActorIdType.IDAM,
                actorId,
                roleType,
                roleName,
                classification,
                GrantType.SPECIFIC,
                RoleCategory.LEGAL_OPERATIONS,
                false,
                attributes
        );
    }
}
