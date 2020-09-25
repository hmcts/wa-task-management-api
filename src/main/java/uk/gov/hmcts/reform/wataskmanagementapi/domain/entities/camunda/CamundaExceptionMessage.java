package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CamundaExceptionMessage {

    private String type;
    private String message;
}
