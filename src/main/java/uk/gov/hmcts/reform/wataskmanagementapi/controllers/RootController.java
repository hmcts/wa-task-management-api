package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    private final String testProperty;

    public RootController(@Value("${testProperty}") String testProperty) {
        this.testProperty = testProperty;
    }

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping(
        path = "/",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> welcome() {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body("Welcome to wa-task-management-api [\"" + testProperty + "\"]");
    }
}
