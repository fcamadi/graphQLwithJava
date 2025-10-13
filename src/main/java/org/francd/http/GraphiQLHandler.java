package org.francd.http;

import org.apache.logging.log4j.core.util.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class GraphiQLHandler extends Handler.Abstract {

    private final String graphQLHtml;

    public GraphiQLHandler() {
        try(var is = GraphiQLHandler.class.getResourceAsStream("/graphiql.html")) {
            assert is != null;
            StringWriter writer = new StringWriter();
            IOUtils.copy(new InputStreamReader(is, StandardCharsets.UTF_8), writer);
            graphQLHtml = writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        response.setStatus(HttpStatus.OK_200);
        response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
        Content.Sink.write(response, true, graphQLHtml, callback);
        return true;
    }
}

