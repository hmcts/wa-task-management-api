package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;

@Slf4j
@Service
@Profile("replica | preview")
public class SubscriptionCreator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;

    String user;
    String password;

    public SubscriptionCreator(@Value("${replication.username}") String user,
                               @Value("${replication.password}") String password) {
        this.user = user;
        this.password = password;
    }

    public void createSubscription() {
        try (Connection connection = dataSource.getConnection();
             Connection connection2 = replicaDataSource.getConnection();) {

            log.info("Primary datasource URL: " + connection.getMetaData().getURL());
            log.info("Replica datasource URL: " + connection2.getMetaData().getURL());

            Properties properties = Driver.parseURL(connection.getMetaData().getURL(), null);
            Properties replicaProperties = Driver.parseURL(connection2.getMetaData().getURL(), null);

            String host = properties.get("PGHOST").toString();
            String port = properties.get("PGPORT").toString();
            String dbName = properties.get("PGDBNAME").toString();

            String replicaHost = replicaProperties.get("PGHOST").toString();
            String replicaPort = replicaProperties.get("PGPORT").toString();
            String replicaDbName = replicaProperties.get("PGDBNAME").toString();

            createSubscription(user, password, host, port, dbName, replicaHost, replicaPort, replicaDbName);
            log.info("Subscription created for: " + host + ":" + port + "/" + dbName);

        } catch (SQLException ex) {
            log.error("Primary datasource connection exception.", ex);
        }
    }

    void createSubscription(String user, String password, String host, String port, String dbName,
                            String replicaHost, String replicaPort, String replicaDbName) {

        String replicaUrl = "jdbc:postgresql://" + host + ":" + replicaPort + "/" + replicaDbName
            + "?user=" + user + "&password=" + password;
        log.info("replicaUrl = " + replicaUrl);

        String subscriptionUrl;
        if ("5432".equals(port)) {
            //hard coded host for local environment, will need fixing when we move to remote environments
            subscriptionUrl = "postgresql://" + host + ":" + port + "/" + dbName
                + "?user=" + user + "&password=" + password;
        } else {
            //this is hard coded for integration test locally
            subscriptionUrl = "postgresql://" + "cft_task_db" + ":" + "5432" + "/" + dbName
                + "?user=" + user + "&password=" + password;
        }

        log.info("subscriptionUrl" + subscriptionUrl);

        String sql = "CREATE SUBSCRIPTION task_subscription CONNECTION '" + subscriptionUrl
            + "' PUBLICATION task_publication WITH (slot_name = main_slot_v1, create_slot = FALSE);";

        sendToDatabase(replicaUrl,sql);
    }

    private void sendToDatabase(String replicaUrl, String sql) {
        try (Connection subscriptionConn = DriverManager.getConnection(replicaUrl);
             Statement subscriptionStatement = subscriptionConn.createStatement();) {

            subscriptionStatement.execute(sql);

            log.info("Subscription created");
        } catch (SQLException e) {
            log.error("Error setting up replication", e);
            throw new ReplicationException("An error occurred during setting up of replication", e);
        }
    }
}
