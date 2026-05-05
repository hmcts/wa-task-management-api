package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime;

import java.time.ZonedDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CamundaTaskInitiationRequest {

    private String name;
    private String assignee;

    @JsonFormat(pattern = CamundaTime.CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime created;

    @JsonFormat(pattern = CamundaTime.CAMUNDA_DATA_TIME_FORMAT)
    private ZonedDateTime due;

    private String description;
    private String processInstanceId;
    private Map<String, CamundaVariable> variables;
}
