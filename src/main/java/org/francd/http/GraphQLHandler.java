package org.francd.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import org.apache.logging.log4j.core.util.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.francd.AsyncGraphQLRuntime;
import org.francd.GraphQLRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphQLHandler extends Handler.Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private GraphQLRuntime graphQLRuntime;
    private AsyncGraphQLRuntime asyncGraphQLRuntime;

    public GraphQLHandler(GraphQLRuntime graphQLRuntime) {
        this.graphQLRuntime = graphQLRuntime;
    }

    public GraphQLHandler(AsyncGraphQLRuntime asyncGraphQLRuntime) {
        this.asyncGraphQLRuntime = asyncGraphQLRuntime;
    }

    @Override
    public boolean handle(Request httpRequest, Response response, Callback callback) throws Exception {
        GraphQLRequest graphQLRequest = graphqlRequestFromHttp(httpRequest);
        String permissionsStr = Optional.ofNullable(httpRequest.getHeaders().get("X-Permissions")).orElse("");
        var permissions = Arrays.stream(permissionsStr.split(",")).map(String::trim).collect(Collectors.toSet());

        ExecutionResult executionResult = null;
        if (graphQLRuntime != null) {
            LOGGER.info("--- START Execution standard query ---------------------------------------------------------");
            executionResult = graphQLRuntime.execute(graphQLRequest.query(), graphQLRequest.variables(), graphQLRequest.operationName(), permissions);
            LOGGER.info("--- END Execution standard query -----------------------------------------------------------");
        }
        if (asyncGraphQLRuntime != null) {
            LOGGER.info("--- START Execution ASYNC query ------------------------------------------------------------");
            executionResult = asyncGraphQLRuntime.execute(graphQLRequest.query(), graphQLRequest.variables(), graphQLRequest.operationName(), permissions);
            LOGGER.info("--- END Execution ASYNC query --------------------------------------------------------------");
        }
        if (graphQLRuntime == null && asyncGraphQLRuntime == null) {
            return false;
        }

        Content.Sink.write(response, true, mapper.writeValueAsString(executionResult.toSpecification()), callback);
        return true;
    }

    private GraphQLRequest graphqlRequestFromHttp(Request httpRequest) throws IOException {

        var contentType = httpRequest.getHeaders().get(HttpHeader.CONTENT_TYPE);
        var mimeType = MimeTypes.getBaseType(contentType);
        var charset = Optional.ofNullable(MimeTypes.getCharsetFromContentType(contentType))
                .orElse(StandardCharsets.UTF_8.name());

        if (mimeType == null) {
            throw new UnsupportedOperationException("Don't know how to handle %s".formatted(contentType));
        }

        InputStream is = Content.Source.asInputStream(httpRequest);
        Reader httpRequestReader = new InputStreamReader(is, charset);
        return switch (mimeType.getBaseType().asString()) {
            case "application/graphql" -> unmarshalGraphQLRequest(httpRequestReader);
            case "application/json" -> unmarshalJSONRequest(httpRequestReader);
            default -> throw new UnsupportedOperationException("Don't know how to handle %s".formatted(contentType));
        };

    }

    private GraphQLRequest unmarshalJSONRequest(Reader jsonContent) throws IOException {
        return mapper.readValue(jsonContent, GraphQLRequest.class);
    }

    private GraphQLRequest unmarshalGraphQLRequest(Reader graphQLcontent) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(graphQLcontent, writer);
        return new GraphQLRequest(writer.toString(), null, null);
    }
}
