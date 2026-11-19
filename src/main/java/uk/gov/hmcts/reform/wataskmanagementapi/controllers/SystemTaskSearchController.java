package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
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
public class SystemTaskSearchController extends BaseController {

    private final IdamService idamService;
    private final SystemTaskSearchService systemTaskSearchService;

    @Operation(description = "Search tasks by case ID within the configured system user's jurisdiction.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "200", description = OK, content = {
        @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = SearchTasksResponse.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ApiResponse(responseCode = "502", description = BAD_GATEWAY)
    @ApiResponse(responseCode = "503", description = "Service Unavailable")
    @GetMapping(path = "/task/searchByCaseId", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchTasksResponse> searchByCaseId(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
        @NotBlank @RequestParam("case_id") String caseId) {
        UserInfo userInfo = idamService.getUserInfoForAuthorization(authToken);
        List<Task> tasks = systemTaskSearchService.searchTasks(userInfo.getEmail(), caseId);
        SearchTasksResponse response = new SearchTasksResponse(tasks);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(response);
    }
}
