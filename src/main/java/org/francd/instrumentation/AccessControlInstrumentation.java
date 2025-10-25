package org.francd.instrumentation;

import graphql.execution.MergedField;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import org.springframework.lang.NonNull;

import java.util.Set;

public class AccessControlInstrumentation extends SimplePerformantInstrumentation {

    //imagine we have two fields that are restricted
    private static final Set<String> RESTRICTED_FIELDS = Set.of("population", "area");

    @Override
    public @NonNull DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters, InstrumentationState state) {

        MergedField field = parameters.getEnvironment().getExecutionStepInfo().getField();
        Set<String> permissions = parameters.getEnvironment().getGraphQlContext().get("permissions");

        if (!permissions.contains("restricted") && RESTRICTED_FIELDS.contains(field.getName())) {
            return (environment -> null);
        }
        return super.instrumentDataFetcher(dataFetcher, parameters, state);
    }
}
