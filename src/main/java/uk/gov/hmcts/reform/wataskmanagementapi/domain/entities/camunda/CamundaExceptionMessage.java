package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import lombok.Data;

@Data
public class CamundaExceptionMessage {

    private String type;
    private String message;
}
