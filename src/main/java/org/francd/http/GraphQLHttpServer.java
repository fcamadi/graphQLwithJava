package org.francd.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.francd.AsyncGraphQLRuntime;
import org.francd.GraphQLRuntime;

import java.sql.Connection;
import java.sql.DriverManager;

public class GraphQLHttpServer {

    private static final int HTTP_PORT = 8080;

    public static void main(String[] args) throws Exception {

        // Set up the DB
        Connection dbConnection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/mondial",
                "postgres",
                "postgres789"
        );

        GraphQLRuntime graphQLRuntime = new GraphQLRuntime(dbConnection);
        AsyncGraphQLRuntime asyncGraphQLRuntime = new AsyncGraphQLRuntime(dbConnection);

        // Set up the HTTP server
        Server server = new Server(HTTP_PORT);

        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.addHandler(new ContextHandler(new GraphiQLHandler(), "/"));
        contextHandlerCollection.addHandler(new ContextHandler(new GraphQLHandler(graphQLRuntime), "/graphql"));
        contextHandlerCollection.addHandler(new ContextHandler(new GraphQLHandler(asyncGraphQLRuntime), "/graphql/async"));
        server.setHandler(contextHandlerCollection);

        // Start the HTTP server
        server.start();
        server.join();
    }
}
