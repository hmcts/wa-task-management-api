package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.usertype.UserType;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;

import java.sql.Types;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@Component
public class CustomPostgreSQL94Dialect extends PostgreSQLDialect implements FunctionContributor {

    public CustomPostgreSQL94Dialect() {
        super();
        this.registerHibernateType(Types.JAVA_OBJECT, NoteResource.class.getName());
        this.registerHibernateType(Types.OTHER, String.class.getName());
        registerFunction("contains_text", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 && ?2::text[]"));
    }

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().register(
            "contains_text",
            new StandardSQLFunction("?1 && ?2::text[]", StandardBasicTypes.BOOLEAN)
        );
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        BasicType<?> noteResourceType = typeContributions.getTypeConfiguration()
            .getBasicTypeRegistry()
            .resolve(
                new SerializableJavaType<>(NoteResource.class)
            );

        typeContributions.getTypeConfiguration().getBasicTypeRegistry().register(noteResourceType);

        typeContributions.getTypeConfiguration().getBasicTypeRegistry().register(StandardBasicTypes.STRING);
    }
}
