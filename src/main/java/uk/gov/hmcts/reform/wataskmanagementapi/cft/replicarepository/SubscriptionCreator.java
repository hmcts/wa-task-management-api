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

    String replicaUser;
    String replicaPassword;
    String primaryUser;
    String primaryPassword;
    String replicaSubscriptionUrl;
    String environment;

    private static final String REFRESH_SUBSCRIPTION = "ALTER SUBSCRIPTION task_subscription REFRESH PUBLICATION;";
    private static final String AND_PASSWORD = "&password=";
    private static final String AND_USER = "?user=";
    public static final String LOCAL_ARM_ARCH = "local-arm-arch";


    public SubscriptionCreator(@Value("${replication.username}") String replicaUser,
                               @Value("${replication.password}") String replicaPassword,
                               @Value("${primary.username}") String primaryUser,
                               @Value("${primary.password}") String primaryPassword,
                               @Value("${replication.subscriptionUrl}") String replicaSubscriptionUrl,
                               @Value("${environment}") String environment) {

        this.replicaUser = replicaUser;
        this.replicaPassword = replicaPassword;
        this.primaryUser = primaryUser;
        this.primaryPassword = primaryPassword;
        this.replicaSubscriptionUrl = replicaSubscriptionUrl;
        this.environment = environment;
    }

    public void createSubscription() {
        try (Connection connection = dataSource.getConnection();
             Connection connection2 = replicaDataSource.getConnection();) {

            log.info("Primary datasource URL: " + connection.getMetaData().getURL());
            log.info("Replica datasource URL: " + connection2.getMetaData().getURL());
            log.info("environment: " + environment);

            Properties properties = Driver.parseURL(connection.getMetaData().getURL(), null);
            Properties replicaProperties = Driver.parseURL(connection2.getMetaData().getURL(), null);

            String host = properties.get("PGHOST").toString();
            String port = properties.get("PGPORT").toString();
            String dbName = properties.get("PGDBNAME").toString();

            String replicaHost = replicaProperties.get("PGHOST").toString();
            String replicaPort = replicaProperties.get("PGPORT").toString();
            String replicaDbName = replicaProperties.get("PGDBNAME").toString();

            createSubscription(host, port, dbName, replicaHost, replicaPort, replicaDbName);
            log.info("Subscription created for: " + host + ":" + port + "/" + dbName);
            log.error("lars-test-01");

        } catch (SQLException ex) {
            log.error("Primary datasource connection exception.", ex);
        }
    }

    void createSubscription(String host, String port, String dbName,
                            String replicaHost, String replicaPort, String replicaDbName) {

        int passwordLength = replicaPassword.length();


        String replicaUrl = "jdbc:postgresql://" + replicaHost + ":" + replicaPort + "/" + replicaDbName
            + AND_USER + replicaUser + AND_PASSWORD + replicaPassword;
        String replicaUrlNoPassword = replicaUrl.substring(0, replicaUrl.length() - passwordLength);
        log.info("replicaUrl = " + replicaUrlNoPassword);

        String subscriptionUrl;
        if (LOCAL_ARM_ARCH.equals(environment)) {
            //this is for integration tests and mac chips
            subscriptionUrl = replicaSubscriptionUrl + "/" + dbName
                + AND_USER + primaryUser + AND_PASSWORD + primaryPassword;
        } else {
            subscriptionUrl = "postgresql://" + host + ":" + port + "/" + dbName
                + AND_USER + primaryUser + AND_PASSWORD + primaryPassword;
        }
        String truncatedSubscriptionUrl = subscriptionUrl.substring(0, subscriptionUrl.length() - passwordLength);

        log.info("subscriptionUrl = " + truncatedSubscriptionUrl);

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

    public void refreshSubscription() {
        try (Connection connection = replicaDataSource.getConnection();) {

            log.info("Replica datasource URL: " + connection.getMetaData().getURL());

            Properties replicaProperties = Driver.parseURL(connection.getMetaData().getURL(), null);

            String replicaHost = replicaProperties.get("PGHOST").toString();
            String replicaPort = replicaProperties.get("PGPORT").toString();
            String replicaDbName = replicaProperties.get("PGDBNAME").toString();

            refreshSubscription(replicaHost, replicaPort, replicaDbName);

        } catch (SQLException ex) {
            log.error("Primary datasource connection exception.", ex);
        }
    }

    private void refreshSubscription(String replicaHost, String replicaPort, String replicaDbName) {
        int passwordLength = replicaPassword.length();
        String replicaUrl = "jdbc:postgresql://" + replicaHost + ":" + replicaPort + "/" + replicaDbName
            + AND_USER + replicaUser + AND_PASSWORD + replicaPassword;
        String replicaUrlNoPassword = replicaUrl.substring(0, replicaUrl.length() - passwordLength);
        log.info("replicaUrl = " + replicaUrlNoPassword);

        try (Connection subscriptionConn = DriverManager.getConnection(replicaUrl);
             Statement subscriptionStatement = subscriptionConn.createStatement();) {

            subscriptionStatement.execute(REFRESH_SUBSCRIPTION);

            log.info("Subscription refreshed");
        } catch (SQLException e) {
            log.error("Error refreshing subscription", e);
            throw new ReplicationException("An error occurred during setting up of replication", e);
        }
    }
}
