package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.type.spi.TypeConfiguration;
import org.springframework.stereotype.Component;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@Component
public class CustomPostgreSQL94Dialect extends PostgreSQLDialect {

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
