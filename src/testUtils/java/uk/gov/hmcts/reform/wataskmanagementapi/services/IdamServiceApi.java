package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.wataskmanagementapi.config.SnakeCaseFeignConfiguration;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "idam-api",
    url = "${idam.api.baseUrl}",
    configuration = SnakeCaseFeignConfiguration.class
)
public interface IdamServiceApi {

    @PostMapping(
        value = "/testing-support/accounts",
        consumes = APPLICATION_JSON_VALUE
    )
    void createTestUser(@RequestBody Map<String, ?> form);


}
