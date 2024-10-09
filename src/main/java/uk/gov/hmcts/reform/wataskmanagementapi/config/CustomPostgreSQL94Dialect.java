package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;

import java.sql.Types;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@Component
public class CustomPostgreSQL94Dialect extends PostgreSQLDialect {

    /*public CustomPostgreSQL94Dialect() {
        super();
        this.registerHibernateType(Types.JAVA_OBJECT, NoteResource.class.getName());
        this.registerHibernateType(Types.OTHER, String.class.getName());
        registerFunction("contains_text", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 && ?2::text[]"));
    }*/

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        TypeConfiguration types = functionContributions.getTypeConfiguration();

        new PatternFunctionDescriptorBuilder(registry, "contains_text", FunctionKind.NORMAL, "?1 && ?2::text[]")
            .setExactArgumentCount(2)
            .setInvariantType(types.getBasicTypeForJavaType(boolean.class))
            .register();
    }
}
