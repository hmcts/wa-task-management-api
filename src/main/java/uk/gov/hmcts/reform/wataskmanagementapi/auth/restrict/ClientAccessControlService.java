package uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ClientAccessControlService {

    private final ServiceAuthTokenValidator serviceAuthTokenValidator;
    private final List<String> privilegedAccessClients;
    private final List<String> exclusiveAccessClients;

    @Autowired
    public ClientAccessControlService(ServiceAuthTokenValidator serviceAuthTokenValidator,
                                      @Value("${config.privilegedAccessClients}") List<String> privilegedAccessClients,
                                      @Value("${config.exclusiveAccessClients}") List<String> exclusiveAccessClients) {
        this.serviceAuthTokenValidator = serviceAuthTokenValidator;
        this.privilegedAccessClients = privilegedAccessClients;
        this.exclusiveAccessClients = exclusiveAccessClients;
    }


    /**
     * Extracts client id from service authorization token and returns if client is whitelisted as privilegedServices.
     * Note: This feature is sitting behind feature flag.
     *
     * @param serviceAuthToken      the service authorization token.
     * @param accessControlResponse the access control response containing userId.
     * @return whether a client has been whitelisted in config.privilegedAccessClients property.
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public boolean hasPrivilegedAccess(String serviceAuthToken, AccessControlResponse accessControlResponse) {
        Objects.requireNonNull(serviceAuthToken, "ServiceAuthorization must not be null");
        Objects.requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId must not be null");

        String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);

        return privilegedAccessClients.contains(serviceName);
    }


    /**
     * Extracts client id from service authorization token and returns if client is whitelisted as exclusiveClient.
     *
     * @param serviceAuthToken the service authorization token.
     * @return whether a client has been whitelisted in config.exclusiveAccessClients property.
     */
    public boolean hasExclusiveAccess(String serviceAuthToken) {
        Objects.requireNonNull(serviceAuthToken, "ServiceAuthorization must not be null");

        String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);

        return exclusiveAccessClients.contains(serviceName);
    }
}
