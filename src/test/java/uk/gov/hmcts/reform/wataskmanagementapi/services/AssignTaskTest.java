package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@MockitoSettings(strictness = Strictness.LENIENT)
class AssignTaskTest extends CamundaServiceBaseTest {

    private AccessControlResponse assignerAccessControlResponse;
    private AccessControlResponse assigneeAccessControlResponse;
    private String taskId;

    @BeforeEach
    void setUp() {
        super.setUp();
        assignerAccessControlResponse = mockAccessControl("some assigner user id");
        assigneeAccessControlResponse = mockAccessControl("some assignee user id");
        taskId = "some task id";

        when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(Collections.emptyMap());
        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(singletonList(MANAGE))))
            .thenReturn(true);
        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(List.of(OWN, EXECUTE))))
            .thenReturn(true);
    }

    @Test
    void assignTask_should_not_call_camunda_if_task_state_already_assigned() {

        Map<String, CamundaVariable> variables = new ConcurrentHashMap<>();
        variables.put("taskState", new CamundaVariable(TaskState.ASSIGNED.value(), "String"));

        when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId))
            .thenReturn(variables);

        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(singletonList(MANAGE))))
            .thenReturn(true);
        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(List.of(OWN, EXECUTE))))
            .thenReturn(true);

        camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            singletonList(MANAGE),
            assigneeAccessControlResponse,
            List.of(OWN, EXECUTE)
        );

        verify(camundaServiceApi).getVariables(
            eq(BEARER_SERVICE_TOKEN),
            eq(taskId)
        );

        verify(camundaServiceApi, never()).addLocalVariablesToTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(taskId),
            any(AddLocalVariableRequest.class)
        );

        verify(camundaServiceApi).assignTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(taskId),
            anyMap()
        );

    }

    @Test
    void assignTask_should_succeed() {

        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(singletonList(MANAGE))))
            .thenReturn(true);
        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(List.of(OWN, EXECUTE))))
            .thenReturn(true);

        camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            singletonList(MANAGE),
            assigneeAccessControlResponse,
            List.of(OWN, EXECUTE)
        );

        verify(camundaServiceApi)
            .addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

        verify(camundaServiceApi)
            .assignTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                anyMap()
            );
    }

    @NotNull
    private AccessControlResponse mockAccessControl(String userId) {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        UserInfo userInfo = UserInfo.builder().uid(userId).build();

        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlResponse.getRoleAssignments())
            .thenReturn(Collections.singletonList(Assignment.builder().build()));
        return accessControlResponse;
    }

    @ParameterizedTest(name = "Scenario {argumentsWithNames}")
    @MethodSource("provideScenario")
    void assignTask_should_throw_exception_when_no_enough_permissions(Scenario scenario) {

        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(singletonList(MANAGE))))
            .thenReturn(scenario.assignerHasAccess);
        when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(List.of(OWN, EXECUTE))))
            .thenReturn(scenario.assigneeHasAccess);

        assertThatThrownBy(() -> camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            singletonList(MANAGE),
            assigneeAccessControlResponse,
            List.of(OWN, EXECUTE)
        ))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage(String.format(
                "User did not have sufficient permissions to assign task with id: %s",
                taskId
            ));

        verify(camundaServiceApi, times(0))
            .addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

        verify(camundaServiceApi, times(0))
            .assignTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                anyMap()
            );
    }

    private static Stream<Scenario> provideScenario() {
        Scenario assignerDoesNotHavePermissions = Scenario.builder()
            .assignerHasAccess(false)
            .assigneeHasAccess(true)
            .build();
        Scenario assigneeDoesNotHavePermissions = Scenario.builder()
            .assignerHasAccess(true)
            .assigneeHasAccess(false)
            .build();

        return Stream.of(
            assignerDoesNotHavePermissions,
            assigneeDoesNotHavePermissions
        );
    }

    @Builder
    static class Scenario {
        boolean assignerHasAccess;
        boolean assigneeHasAccess;
    }


    @Test
    void assignTask_should_throw_server_error_exception_when_assignTask_fails() {

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase()
            );

        doThrow(exception)
            .when(camundaServiceApi).assignTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());

        assertThatThrownBy(() -> camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            singletonList(MANAGE),
            assigneeAccessControlResponse,
            List.of(OWN, EXECUTE)
        ))
            .isInstanceOf(ServerErrorException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage(
                String.format(
                    "There was a problem assigning the task with id: %s",
                    taskId
                )
            );

    }

    @Test
    void assignTask_should_throw_server_error_exception_exception_when_addLocalVariablesToTask_fails() {

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                createCamundaTestException("aCamundaErrorType", "some exception message")
            );

        doThrow(exception).when(camundaServiceApi).addLocalVariablesToTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(taskId),
            any(AddLocalVariableRequest.class)
        );

        assertThatThrownBy(() -> camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            singletonList(MANAGE),
            assigneeAccessControlResponse,
            List.of(OWN, EXECUTE)
        ))
            .isInstanceOf(ServerErrorException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage(
                String.format(
                    "There was a problem assigning the task with id: %s",
                    taskId
                )
            );
    }


}
