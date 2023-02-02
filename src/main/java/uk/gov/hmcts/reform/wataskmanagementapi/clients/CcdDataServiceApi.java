package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamelCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "ccd-client",
    url = "${core_case_data.api.url}",
    configuration = CamelCaseFeignConfiguration.class
)
public interface CcdDataServiceApi {
    String EXPERIMENTAL = "experimental=true";

    @GetMapping(
        path = "/cases/{cid}",
        headers = EXPERIMENTAL
    )
    CaseDetails getCase(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("cid") String caseId
    );

}
