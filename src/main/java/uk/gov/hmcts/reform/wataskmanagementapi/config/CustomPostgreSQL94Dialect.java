package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.hibernate.dialect.PostgreSQL10Dialect;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.Notes;

import java.sql.Types;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CustomPostgreSQL94Dialect extends PostgreSQL10Dialect {

    public CustomPostgreSQL94Dialect() {
        super();
        this.registerHibernateType(Types.JAVA_OBJECT, Notes.class.getName());
    }
}
