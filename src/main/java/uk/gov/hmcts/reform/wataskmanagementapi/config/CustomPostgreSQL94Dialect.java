package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;

import java.sql.Types;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@Component
public class CustomPostgreSQL94Dialect extends PostgreSQL10Dialect {

    public CustomPostgreSQL94Dialect() {
        super();
        this.registerHibernateType(Types.JAVA_OBJECT, NoteResource.class.getName());
        registerFunction("contains_text", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 && ?2::text[]"));
    }
}
