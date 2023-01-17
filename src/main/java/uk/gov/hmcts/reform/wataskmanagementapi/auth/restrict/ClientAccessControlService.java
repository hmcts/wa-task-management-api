package uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ClientAccessControlService {

    private final ServiceAuthTokenValidator serviceAuthTokenValidator;
    private final List<String> exclusiveAccessClients;

    @Autowired
    public ClientAccessControlService(ServiceAuthTokenValidator serviceAuthTokenValidator,
                                      @Value("${config.exclusiveAccessClients}") List<String> exclusiveAccessClients) {
        this.serviceAuthTokenValidator = serviceAuthTokenValidator;
        this.exclusiveAccessClients = exclusiveAccessClients;
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
