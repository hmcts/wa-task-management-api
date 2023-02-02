package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.document.utils.InMemoryMultipartFile;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.documents.Document;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Service
public class DocumentManagementUploader {

    @Autowired
    private DocumentUploadClientApi documentUploadClientApi;

    public Document upload(
        Resource resource,
        String contentType,
        String accessToken,
        String serviceAuthorizationToken,
        UserInfo userInfo) {

        try {

            MultipartFile file = new InMemoryMultipartFile(
                resource.getFilename(),
                resource.getFilename(),
                contentType,
                ByteStreams.toByteArray(resource.getInputStream())
            );

            log.info("Uploading document '{}'", file.getOriginalFilename());
            UploadResponse uploadResponse =
                documentUploadClientApi
                    .upload(
                        accessToken,
                        serviceAuthorizationToken,
                        userInfo.getUid(),
                        Collections.singletonList(file)
                    );

            uk.gov.hmcts.reform.document.domain.Document uploadedDocument =
                uploadResponse
                    .getEmbedded()
                    .getDocuments()
                    .get(0);

            log.info("Document '{}' uploaded successfully", file.getOriginalFilename());
            return new Document(
                uploadedDocument
                    .links
                    .self
                    .href,
                uploadedDocument
                    .links
                    .binary
                    .href,
                file.getOriginalFilename()
            );

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
