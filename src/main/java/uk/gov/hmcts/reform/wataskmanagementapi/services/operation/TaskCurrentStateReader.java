package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.Optional;

@Component
public class TaskCurrentStateReader {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public TaskCurrentStateReader(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<CFTTaskState> findCurrentState(String taskId) {
        return cftTaskDatabaseService.findByIdOnly(taskId)
            .map(taskResource -> taskResource.getState());
    }
}
