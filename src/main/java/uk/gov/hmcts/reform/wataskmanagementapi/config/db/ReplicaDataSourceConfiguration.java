package uk.gov.hmcts.reform.wataskmanagementapi.config.db;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "replicaEntityManagerFactory",
    basePackages = {"uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository"}
)
@Profile("replica | preview")
public class ReplicaDataSourceConfiguration {

    @Bean(name = "replicaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource-replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "replicaEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean replicaEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("replicaDataSource") DataSource dataSource) {
        return
            builder
                .dataSource(dataSource)
                .packages("uk.gov.hmcts.reform.wataskmanagementapi.entity",
                          "uk.gov.hmcts.reform.wataskmanagementapi.entity.replica")
                .persistenceUnit("cft_task_db_replica")
                .build();
    }

    @Bean(name = "replicaTransactionManager")
    public PlatformTransactionManager replicaTransactionManager(
        @Qualifier("replicaEntityManagerFactory") EntityManagerFactory replicaEntityManagerFactory) {
        return new JpaTransactionManager(replicaEntityManagerFactory);
    }
}
