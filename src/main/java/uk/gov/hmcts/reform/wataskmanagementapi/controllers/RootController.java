package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    private static final Logger LOG = getLogger(RootController.class);
    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final String MESSAGE = "Welcome to wa-task-management-api";

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
        LOG.info("Welcome message '{}' from running instance: {}", MESSAGE, INSTANCE_ID);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(MESSAGE);
    }
}
