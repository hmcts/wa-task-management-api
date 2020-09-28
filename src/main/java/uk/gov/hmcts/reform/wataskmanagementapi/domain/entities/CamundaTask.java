package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import io.swagger.annotations.ApiModel;

@ApiModel("Task")
public class CamundaTask {

    private final String id;

    public CamundaTask(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
