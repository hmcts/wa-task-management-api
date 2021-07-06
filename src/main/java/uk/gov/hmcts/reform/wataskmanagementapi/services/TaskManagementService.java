package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;

@Slf4j
@Service
public class TaskManagementService {

    private final CamundaService camundaService;

    @Autowired
    public TaskManagementService(CamundaService camundaService) {
        this.camundaService = camundaService;
    }

    public void terminateTask(String taskId, TerminateInfo terminateInfo) {

        /*
        Complete or cancel task in Camunda
        The Camunda API is used to complete or cancel the task within Camunda
        If Camunda process succeeds, commit changes to CFT DB
        If Camunda process fails - roll back
         */

        switch (terminateInfo.getTerminateReason()) {

            //TODO: Refactor the Camunda service to abstract all constraints and restrictions checking into this class
            //Todo: Should abort early
            case COMPLETED:
                obtainLock(taskId);
                updateCftTask(TaskState.COMPLETED);
                //If this call fails roll back transaction.
                camundaService.completeTask();
                break;
            case CANCELLED:
                obtainLock(taskId);
                updateCftTask(TaskState.CANCELLED);
                camundaService.cancelTask();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + terminateInfo.getTerminateReason());
        }
    }

    public void initiateTask(String taskId, InitiateTaskRequest initiateTaskRequest) {

        //TODO: Save task into CFT Task db

    }

    private void obtainLock(String taskId) {
        /*TODO:
        Lock & update task - lock it so no changes can be made to the task by ay other transaction
        All CFT Task DB changes are made before the Camunda API call, to ensure that all database constraints are applied
        and that only a technical failure can prevent the subsequent commit of the changes
        */
        return;
    }

    private void updateCftTask(TaskState taskState) {
        return;
    }


}
