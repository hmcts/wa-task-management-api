package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import static java.util.Objects.requireNonNull;

@Component
@Slf4j
public class IdamService {

    private final IdamWebApi idamWebApi;

    @Autowired
    public IdamService(IdamWebApi idamWebApi) {
        this.idamWebApi = idamWebApi;
    }

    @Cacheable(value = "idam_user_info_cache", key = "#accessToken", sync = true)
    public UserInfo getUserInfo(String accessToken) {
        requireNonNull(accessToken, "access token must not be null");
        return idamWebApi.userInfo(accessToken);
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    public UserInfo getUserInfoForAuthorization(String accessToken) {
        UserInfo userInfo;
        try {
            userInfo = idamWebApi.userInfo(accessToken);
        } catch (FeignException exception) {
            HttpStatus status = HttpStatus.resolve(exception.status());
            // Rate limiting does not mean that the user is unavailable.
            if (status != null && status.is4xxClientError() && status != HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("IDAM user lookup rejected with status {}", exception.status(), exception);
                // A Feign cause would be handled as a downstream error by the global controller advice.
                throw new UnAuthorizedException("IDAM user not available");
            }
            throw exception;
        }
        if (userInfo == null || !StringUtils.hasText(userInfo.getUid())
            || !StringUtils.hasText(userInfo.getEmail())) {
            throw new UnAuthorizedException("IDAM user not available");
        }
        return userInfo;
    }

    @Cacheable(value = "idam_user_id_cache", key = "#accessToken", sync = true)
    public String getUserId(String accessToken) {
        UserInfo userInfo = getUserInfo(accessToken);
        requireNonNull(userInfo.getUid(), "User id must not be null");
        return userInfo.getUid();
    }
}
