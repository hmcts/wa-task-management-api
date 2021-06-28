package uk.gov.hmcts.reform.wataskmanagementapi.auth.privilege;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PrivilegedAccessControlService {

    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final ServiceAuthTokenValidator serviceAuthTokenValidator;
    private final List<String> privilegedServices;

    @Autowired
    public PrivilegedAccessControlService(ServiceAuthTokenValidator serviceAuthTokenValidator,
                                          LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                          @Value("${config.privilegedClients}") List<String> privilegedServices) {
        this.serviceAuthTokenValidator = serviceAuthTokenValidator;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.privilegedServices = privilegedServices;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public boolean hasPrivilegedAccess(String serviceAuthToken, AccessControlResponse accessControlResponse) {
        Objects.requireNonNull(serviceAuthToken, "ServiceAuthorization must not be null");
        Objects.requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId must not be null");

        boolean isPrivilegedClient = false;

        boolean isFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.PRIVILEGED_ACCESS_FEATURE,
            accessControlResponse.getUserInfo().getUid()
        );

        if (isFeatureEnabled) {
            String serviceName = serviceAuthTokenValidator.getServiceName(serviceAuthToken);

            isPrivilegedClient = privilegedServices.contains(serviceName);
        }
        return isPrivilegedClient;
    }
}
