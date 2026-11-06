package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityConfigurationTaskInitiationTestController {

    @PostMapping("/task/{taskId}/initiation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void initiate() {
        // An empty handler lets the test distinguish a permitted request from an unauthorized one.
    }
}
