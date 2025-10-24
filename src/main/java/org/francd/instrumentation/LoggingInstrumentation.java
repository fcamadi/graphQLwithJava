package org.francd.instrumentation;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Document;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.whenCompleted;

@SuppressWarnings("NullableProblems")
public class LoggingInstrumentation extends SimplePerformantInstrumentation {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInstrumentation.class);

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters, InstrumentationState state) {
        return logState("execution");
    }


    @Override
    public InstrumentationContext<Document> beginParse(InstrumentationExecutionParameters parameters, InstrumentationState state) {
        return logState("parsing");
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters, InstrumentationState state) {
        return logState("validating");
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        return logState("operation execution");
    }

    @Override
    public ExecutionStrategyInstrumentationContext beginExecutionStrategy(InstrumentationExecutionStrategyParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStrategyParameters().getExecutionStepInfo();
        LOGGER.info("Starting state [{}] for path <{}>", state, stepInfo);
        return new ExecutionStrategyInstrumentationContext() {
            @Override
            public void onDispatched(CompletableFuture<ExecutionResult> result) {}

            @Override
            public void onCompleted(ExecutionResult result, Throwable t) {
                LOGGER.info("Completed state [{}] for path <{}>", state, stepInfo);
            }
        };
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStepInfo();
        return logState("field", stepInfo);
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStepInfo();
        return logState("field fetching", stepInfo);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginFieldComplete(InstrumentationFieldCompleteParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStepInfo();
        return logState("field fetching", stepInfo);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginFieldListComplete(InstrumentationFieldCompleteParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStepInfo();
        return logState("field fetching", stepInfo);
    }

    @Override
    public ExecutionInput instrumentExecutionInput(ExecutionInput executionInput, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        LOGGER.info("Instrumenting state execution input");
        return super.instrumentExecutionInput(executionInput, parameters, state);
    }

    @Override
    public DocumentAndVariables instrumentDocumentAndVariables(DocumentAndVariables documentAndVariables, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        LOGGER.info("Instrumenting document and variables");
        return super.instrumentDocumentAndVariables(documentAndVariables, parameters, state);
    }

    @Override
    public GraphQLSchema instrumentSchema(GraphQLSchema schema, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        LOGGER.info("Instrumenting schema");
        return super.instrumentSchema(schema, parameters, state);
    }

    @Override
    public ExecutionContext instrumentExecutionContext(ExecutionContext executionContext, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        LOGGER.info("Instrumenting execution context");
        return super.instrumentExecutionContext(executionContext, parameters, state);
    }

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
        var stepInfo = parameters.getExecutionStepInfo();
        LOGGER.info("Instrumenting data fetcher for step ({})", stepInfo);
        return super.instrumentDataFetcher(dataFetcher, parameters, state);
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        LOGGER.info("Instrumenting execution result");
        return CompletableFuture.completedFuture(ExecutionResult.newExecutionResult().from(executionResult)
                .addExtension("loggingEnabled", true)
                .build());
    }

    private <T> InstrumentationContext<T> logState(String state, ExecutionStepInfo stepInfo) {
        LOGGER.info("Starting state [{}] for path <{}>", state, stepInfo);
        return whenCompleted((r,e) -> LOGGER.info("Completed state [{}] for path <{}>.", state, stepInfo));
    }

    private static <T> InstrumentationContext<T> logState(String state) {
        LOGGER.info("Starting state [{}]", state);
        return whenCompleted((r,e) -> LOGGER.info("Completed state [{}].", state));
    }


}
