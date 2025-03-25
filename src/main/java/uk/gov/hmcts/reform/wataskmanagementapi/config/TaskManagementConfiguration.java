package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "config")
public class TaskManagementConfiguration {
    private boolean updateCompletionProcessFlagEnabled;

    public boolean isUpdateCompletionProcessFlagEnabled() {
        return updateCompletionProcessFlagEnabled;
    }

    public void setUpdateCompletionProcessFlagEnabled(boolean updateCompletionProcessFlagEnabled) {
        this.updateCompletionProcessFlagEnabled = updateCompletionProcessFlagEnabled;
    }
}

