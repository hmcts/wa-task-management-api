package uk.gov.hmcts.reform.wataskmanagementapi.config.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayReplicaMigrationConfiguration {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataSource replicaDataSource;

    @Bean
    public FlywayMigrationStrategy multiDBMigrateStrategy() {
        FlywayMigrationStrategy strategy = new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                Flyway flywayBase = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas("cft_task_db")
                    .defaultSchema("cft_task_db")
                    .locations("db/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.LATEST).load();

                flywayBase.migrate();

                Flyway flywayReplica = Flyway.configure()
                    .dataSource(replicaDataSource)
                    .schemas("cft_task_db")
                    .defaultSchema("cft_task_db")
                    .locations("dbreplica/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.LATEST).load();

                flywayReplica.migrate();

            }
        };

        return strategy;
    }
}
