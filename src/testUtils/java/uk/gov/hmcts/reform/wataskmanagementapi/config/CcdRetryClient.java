package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.documents.Document;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.documents.DocumentNames.NOTICE_OF_APPEAL_PDF;

@Service
@Slf4j
public class CcdRetryClient {
    private final CoreCaseDataApi coreCaseDataApi;

    private final AuthorizationProvider authorizationProvider;
    private final DocumentManagementFiles documentManagementFiles;

    public CcdRetryClient(CoreCaseDataApi coreCaseDataApi,
                          AuthorizationProvider authorizationProvider,
                          DocumentManagementFiles documentManagementFiles) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.authorizationProvider = authorizationProvider;
        this.documentManagementFiles = documentManagementFiles;
    }

    @Retryable(value = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public String createCCDCaseWithJurisdictionAndCaseTypeAndEvent(String jurisdiction,
                                                                    String caseType,
                                                                    String startEventId,
                                                                    String submitEventId,
                                                                    TestAuthenticationCredentials credentials,
                                                                    String resourceFilename) {

        String userToken = credentials.getHeaders().getValue(AUTHORIZATION);
        String serviceToken = credentials.getHeaders().getValue(SERVICE_AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(userToken);

        Document document = documentManagementFiles.getDocumentAs(NOTICE_OF_APPEAL_PDF, credentials);

        StartEventResponse startCase = coreCaseDataApi.startForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            startEventId
        );

        Map data = null;
        try {
            String caseDataString = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:" + resourceFilename),
                "UTF-8"
            );

            caseDataString = caseDataString.replace(
                "{NEXT_HEARING_DATE}",
                OffsetDateTime.now().toString()
            );

            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_STORE_URL}",
                document.getDocumentUrl()
            );
            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_NAME}",
                document.getDocumentFilename()
            );
            caseDataString = caseDataString.replace(
                "{NOTICE_OF_DECISION_DOCUMENT_STORE_URL_BINARY}",
                document.getDocumentBinaryUrl()
            );

            data = new ObjectMapper().readValue(caseDataString, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startCase.getToken())
            .event(Event.builder()
                       .id(startCase.getEventId())
                       .summary("summary")
                       .description("description")
                       .build())
            .data(data)
            .build();

        //Fire submit event
        CaseDetails caseDetails = coreCaseDataApi.submitForCaseworker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            true,
            caseDataContent
        );

        log.info("Created case [" + caseDetails.getId() + "]");

        StartEventResponse submitCase = coreCaseDataApi.startEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            caseDetails.getId().toString(),
            submitEventId
        );

        CaseDataContent submitCaseDataContent = CaseDataContent.builder()
            .eventToken(submitCase.getToken())
            .event(Event.builder()
                       .id(submitCase.getEventId())
                       .summary("summary")
                       .description("description")
                       .build())
            .data(data)
            .build();

        coreCaseDataApi.submitEventForCaseWorker(
            userToken,
            serviceToken,
            userInfo.getUid(),
            jurisdiction,
            caseType,
            caseDetails.getId().toString(),
            true,
            submitCaseDataContent
        );
        log.info("Submitted case [" + caseDetails.getId() + "]");

        authorizationProvider.deleteAccount(credentials.getAccount().getUsername());

        return caseDetails.getId().toString();
    }

}
