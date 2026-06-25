package uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.config.ClientAccessProperties;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClientAccessControlService {

    public static final String MSG_SRV_AUTH_MUST_NOT_BE_NULL = "ServiceAuthorization must not be null";

    private final ServiceAuthTokenValidator serviceAuthTokenValidator;
    private final List<String> privilegedAccessClients;
    private final List<String> exclusiveAccessClients;
    private final Map<String, List<String>> serviceCaseTypeAccess;

    public ClientAccessControlService(ServiceAuthTokenValidator serviceAuthTokenValidator,
                                      ClientAccessProperties clientAccessProperties) {
        this.serviceAuthTokenValidator = serviceAuthTokenValidator;
        this.privilegedAccessClients = clientAccessProperties.getPrivilegedAccessClients();
        this.exclusiveAccessClients = clientAccessProperties.getExclusiveAccessClients();
        this.serviceCaseTypeAccess = clientAccessProperties.getServiceCaseTypeAccess();
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
        Objects.requireNonNull(serviceAuthToken, MSG_SRV_AUTH_MUST_NOT_BE_NULL);
        Objects.requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId must not be null");

        String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);

        return privilegedAccessClients.contains(serviceName);
    }

    /**
     * Extracts client id from service authorization token and returns if client is whitelisted as PrivilegedAccess.
     *
     * @param serviceAuthToken the service authorization token.
     * @return whether a client has been whitelisted in config.hasPrivilegedAccess property.
     */
    public boolean hasPrivilegedAccess(String serviceAuthToken) {
        Objects.requireNonNull(serviceAuthToken, MSG_SRV_AUTH_MUST_NOT_BE_NULL);

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
        Objects.requireNonNull(serviceAuthToken, MSG_SRV_AUTH_MUST_NOT_BE_NULL);

        String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);

        return exclusiveAccessClients.contains(serviceName);
    }

    public boolean hasExclusiveCaseTypeAccess(String serviceAuthToken, String caseTypeId) {
        Objects.requireNonNull(serviceAuthToken, MSG_SRV_AUTH_MUST_NOT_BE_NULL);
        if (!StringUtils.hasText(caseTypeId)) {
            return false;
        }

        String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);
        if (!exclusiveAccessClients.contains(serviceName)) {
            return false;
        }

        List<String> allowedCaseTypes = serviceCaseTypeAccess.get(serviceName);

        return allowedCaseTypes == null || allowedCaseTypes.contains(caseTypeId);
    }
}
