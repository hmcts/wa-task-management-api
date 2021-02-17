package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.wataskmanagementapi.config.FeignConfiguration;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "idam-api",
    url = "${idam.api.baseUrl}",
    configuration = FeignConfiguration.class
)
public interface IdamServiceApi {

    @PostMapping(
        value = "/testing-support/accounts",
        produces = APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    void createTestUser(@RequestBody Map<String, ?> form);


}
