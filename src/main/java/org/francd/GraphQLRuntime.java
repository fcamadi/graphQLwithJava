package org.francd;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.francd.fetchers.*;
import org.francd.model.Continent;
import org.francd.model.Country;
import org.francd.model.Province;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

public class GraphQLRuntime {

    private final GraphQL graphql;

    public GraphQLRuntime(Connection dbConnection) throws IOException {
        // The SchemaGenerator is responsible for turning a type registry and wiring into an executable GraphQLSchema
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        // buildTypeDefinitionRegistry() reads the SDL files (e.g., *.graphqls) and
        // registers all object types, enums, inputs, etc., in a TypeDefinitionRegistry.
        TypeDefinitionRegistry typeRegistry = buildTypeDefinitionRegistry();
        // buildRuntimeWiring() maps each field in the schema to a DataFetcher – the Java code
        // that actually supplies the data when a query asks for that field.
        RuntimeWiring wiring = buildRuntimeWiring(dbConnection);
        // The generator combines the type definitions with the wiring, producing a GraphQLSchema
        // that knows both the shape of the API and how to fetch data.
        GraphQLSchema schema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
        // This builds a GraphQL object that can later be called with execute(query)
        // to run queries against the schema.
        graphql = GraphQL.newGraphQL(schema).build();
    }

    private TypeDefinitionRegistry buildTypeDefinitionRegistry() throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        try(InputStream is = GraphQLRuntime.class.getResourceAsStream("/schema.graphql")) {
            assert is != null;
            Reader schemaReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            return schemaParser.parse(schemaReader);
        }
    }

    private RuntimeWiring buildRuntimeWiring(Connection dbConnection) {
        return RuntimeWiring.newRuntimeWiring()
                //Wire Enums
                .type("Continent", builder -> builder.enumValues(new NaturalEnumValuesProvider<>(Continent.class)))
                 //Wire Data Fetchers
                .type("Query", builder -> builder.dataFetcher("countries", new DBCountriesDataFetcher(dbConnection)))
                .type("Query", builder -> builder.dataFetcher("country", new DBOneCountryDataFetcher(dbConnection)))
                .type("Query", builder -> builder.dataFetcher("provinces", new DBProvincesOfCountryDataFetcher(dbConnection)))
                .type("Country",  builder -> builder.dataFetcher("capital", new DBCityDataFetcher<>(dbConnection,Country::capital)))
                .type("Country",  builder -> builder.dataFetcher("provinces", new DBProvincesOfCountryDataFetcher(dbConnection)))
                .type("Province", builder -> builder.dataFetcher("capital", new DBCityDataFetcher<>(dbConnection,Province::capital)))
                .type("City",  builder -> builder.dataFetcher("province", new DBProvinceFromCapitalDataFetcher(dbConnection)))
                .build();
    }

    public ExecutionResult execute(String query) {
        return execute(query,null,null);
    }

    public ExecutionResult execute(String query, Map<String, Object> variables, String operationName) {
        ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput()
                .query(query);
        if (variables != null) {
            executionInputBuilder.variables(variables);
        }
        if (operationName != null) {
            executionInputBuilder.operationName(operationName);
        }
        return graphql.execute(executionInputBuilder.build());
    }
}

