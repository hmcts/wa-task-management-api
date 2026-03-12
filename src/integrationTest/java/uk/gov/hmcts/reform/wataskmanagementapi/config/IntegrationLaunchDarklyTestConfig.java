package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@TestConfiguration
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class IntegrationLaunchDarklyTestConfig {

    @Bean
    static BeanDefinitionRegistryPostProcessor launchDarklyOverridePostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("ldClient")) {
                    registry.removeBeanDefinition("ldClient");
                }
                RootBeanDefinition definition = new RootBeanDefinition(LDClientInterface.class);
                definition.setPrimary(true);
                definition.setInstanceSupplier(IntegrationLaunchDarklyTestConfig::createMockClient);
                registry.registerBeanDefinition("ldClient", definition);
            }

            @Override
            public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory
            ) {
                // No-op
            }

        };
    }

    private static LDClientInterface createMockClient() {
        LDClientInterface ldClient = Mockito.mock(LDClientInterface.class);
        lenient().when(ldClient.boolVariation(anyString(), any(LDContext.class), anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(2));
        lenient().when(ldClient.jsonValueVariation(anyString(), any(LDContext.class), any(LDValue.class)))
            .thenAnswer(invocation -> invocation.getArgument(2));
        return ldClient;
    }
}
