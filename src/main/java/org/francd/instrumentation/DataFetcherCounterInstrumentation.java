package org.francd.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DataFetcherCounterInstrumentation extends SimplePerformantInstrumentation {

    private static class DataFetcherUsageState implements InstrumentationState {

        final Map<String, Integer> usages = new ConcurrentHashMap<>();

        public void increaseUsageFor(DataFetcher<?> dataFetcher) {
            synchronized (usages) {
                String dataFetcherClass = dataFetcher.getClass().getSimpleName();
                int prevCount = usages.getOrDefault(dataFetcherClass, 0);
                usages.put(dataFetcherClass, prevCount + 1);
            }
        }

        public Map<String, Integer> getUsages() {
            return usages;
        }
    }

    @Override
    public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
        return new DataFetcherUsageState();
    }

    @Override
    public @NonNull DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
        if(state instanceof DataFetcherUsageState usageState) {
            usageState.increaseUsageFor(dataFetcher);
            return dataFetcher;
        } else {
            throw new RuntimeException("GraphQL didn't give us back a proper state");
        }
    }

    @Override
    public @NonNull CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        if(state instanceof DataFetcherUsageState usageState) {
            return CompletableFuture.completedFuture(executionResult.transform(builder -> builder.addExtension(
                    "dataFetcherUsage", usageState.getUsages()
            )));
        } else {
            throw new RuntimeException("GraphQL didn't give us back a proper state");
        }
    }
}
