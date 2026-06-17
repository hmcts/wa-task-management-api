package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GetTasksResponse {

    List<GetTaskResponseItem> tasks;
    long totalRecords;
}
