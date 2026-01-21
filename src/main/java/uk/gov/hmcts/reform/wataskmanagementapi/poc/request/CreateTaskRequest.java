package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequestTask;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateTaskRequest
 */
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T13:27:51.996306Z[Europe/London]")public class CreateTaskRequest {

  private CreateTaskRequestTask task;

  public CreateTaskRequest task(CreateTaskRequestTask task) {
    this.task = task;
    return this;
  }

  /**
   * Get task
   * @return task
  */
  
  @Schema(name = "task", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("task")
  public CreateTaskRequestTask getTask() {
    return task;
  }

  public void setTask(CreateTaskRequestTask task) {
    this.task = task;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateTaskRequest createTaskRequest = (CreateTaskRequest) o;
    return Objects.equals(this.task, createTaskRequest.task);
  }

  @Override
  public int hashCode() {
    return Objects.hash(task);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTaskRequest {\n");
    sb.append("    task: ").append(toIndentedString(task)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

