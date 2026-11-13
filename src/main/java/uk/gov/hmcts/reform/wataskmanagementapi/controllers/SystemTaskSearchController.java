package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.SearchTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemTaskSearchService;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@RestController
@Validated
@RequiredArgsConstructor
public class SystemTaskSearchController {

    private final SystemTaskSearchService systemTaskSearchService;

    @Operation(description = "Search tasks by case ID within the configured system user's jurisdiction.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @GetMapping(path = "/task/search", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchTasksResponse> searchTasks(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
        @NotBlank @RequestParam("case_id") String caseId) {
        List<Task> tasks = systemTaskSearchService.searchTasks(authToken, caseId);
        SearchTasksResponse response = new SearchTasksResponse(tasks);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}
