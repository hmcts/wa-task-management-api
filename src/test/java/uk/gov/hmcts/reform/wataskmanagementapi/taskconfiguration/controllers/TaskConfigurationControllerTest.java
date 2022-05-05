package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions.ConfigureTaskException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;

@ExtendWith(MockitoExtension.class)
class TaskConfigurationControllerTest {

    @Mock
    private ConfigureTaskService configureTaskService;

    private String caseId = UUID.randomUUID().toString();
    private String taskName;

    private TaskConfigurationController taskConfigurationController;

    @BeforeEach
    void setUp() {

        taskConfigurationController = new TaskConfigurationController(configureTaskService);

        caseId = UUID.randomUUID().toString();

        taskName = "processApplication";
    }

    @Test
    void should_configure_task() {

        final String taskId = UUID.randomUUID().toString();

        final ResponseEntity<String> response = taskConfigurationController.configureTask(taskId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(configureTaskService, times(1))
            .configureTask(taskId);
    }

    @Test
    void should_get_configuration_for_task() {

        final String taskId = UUID.randomUUID().toString();

        final ConfigureTaskRequest configureTaskRequest = getConfigureTaskRequest();

        final ResponseEntity<ConfigureTaskResponse> response =
            taskConfigurationController.getConfigurationForTask(
                taskId,
                configureTaskRequest);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void should_return_404_when_an_error_occurred() {
        final String taskId = UUID.randomUUID().toString();

        doThrow(ConfigureTaskException.class)
            .when(configureTaskService)
            .configureTask(taskId);

        final ResponseEntity<String> response = taskConfigurationController.configureTask(taskId);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    private ConfigureTaskRequest getConfigureTaskRequest() {


        return new ConfigureTaskRequest(
            Map.of(
                CASE_ID.value(), caseId,
                TASK_NAME.value(), taskName
            )
        );
    }
}
