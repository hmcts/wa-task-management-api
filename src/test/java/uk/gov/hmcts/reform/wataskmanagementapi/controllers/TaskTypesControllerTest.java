package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskTypeResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskTypesService;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTypesControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private TaskTypesService taskTypesService;

    private TaskTypesController taskTypesController;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        taskTypesController = new TaskTypesController(
            accessControlService,
            taskTypesService
        );

        userInfo = UserInfo.builder()
            .roles(new ArrayList<>(asList("Role1", "Role2")))
            .build();
    }

    @Test
    void should_return_task_type() {

        RoleAssignment roleAssignment = new RoleAssignment(
            ActorIdType.IDAM,
            "1258555",
            RoleType.CASE,
            "Judge",
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.JUDICIAL,
            false,
            null
        );

        List<RoleAssignment> roleAssignmentList = singletonList(roleAssignment);
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignmentList);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(accessControlResponse);

        TaskType taskType = new TaskType("taskTypeId1", "Task type name");
        TaskTypeResponse taskTypeResponse = new TaskTypeResponse(taskType);
        List<TaskTypeResponse> taskTypeResponses = List.of(taskTypeResponse);
        GetTaskTypesResponse getTaskTypesResponse = new GetTaskTypesResponse(taskTypeResponses);
        when(taskTypesService.getTaskTypes(accessControlResponse, "wa"))
            .thenReturn(getTaskTypesResponse);

        ResponseEntity<GetTaskTypesResponse> response =
            taskTypesController.getTaskTypes(IDAM_AUTH_TOKEN, "wa");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetTaskTypesResponse.class));
        assertNotNull(response.getBody());

        assertEquals(response.getBody(), new GetTaskTypesResponse(taskTypeResponses));

        verify(taskTypesService, times(1))
            .getTaskTypes(accessControlResponse, "wa");
    }

}
