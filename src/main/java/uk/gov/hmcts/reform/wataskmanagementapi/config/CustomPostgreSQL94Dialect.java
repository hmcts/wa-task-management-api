package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;

import java.sql.Types;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@Component
public class CustomPostgreSQL94Dialect extends SQLServerDialect {

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

    /*@Override
    public JdbcType resolveSqlTypeDescriptor(
        String columnTypeName,
        int jdbcTypeCode,
        int precision,
        int scale,
        JdbcTypeRegistry jdbcTypeRegistry) {

        switch ( jdbcTypeCode ) {
            case Types.JAVA_OBJECT:
                jdbcTypeCode = NoteResource.class.;
                break;
            case Types.OTHER:
                jdbcTypeCode = String.;
        }
        return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
    }*/



}
