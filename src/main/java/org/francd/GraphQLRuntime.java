package org.francd;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.francd.fetchers.*;
import org.francd.instrumentation.DataFetcherCounterInstrumentation;
import org.francd.instrumentation.LoggingInstrumentation;
import org.francd.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import static graphql.schema.AsyncDataFetcher.async;

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

        // To add more than one instrumentation class:
        ChainedInstrumentation chainedInstrumentations = new ChainedInstrumentation(
                new LoggingInstrumentation(),
                new DataFetcherCounterInstrumentation()
        );

        graphql = GraphQL.newGraphQL(schema)
                //.instrumentation(new LoggingInstrumentation())
                //.instrumentation(new AccessControlInstrumentation())
                //.instrumentation(new DataFetcherCounterInstrumentation())  // the last one is the only one applied
                .instrumentation(chainedInstrumentations)
                .build();
    }

    //We read and parse the schema
    private TypeDefinitionRegistry buildTypeDefinitionRegistry() throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry;
        try(InputStream is = GraphQLRuntime.class.getResourceAsStream("/schema.graphql")) {
            assert is != null;
            Reader schemaReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            typeRegistry = schemaParser.parse(schemaReader);
        }

        //But we add a "Province" programmatically
        typeRegistry.add(TypeHelper.objectDefinitionOf(Province.class));
        // We add also the City
        typeRegistry.add(TypeHelper.objectDefinitionOf(City.class));

        return typeRegistry;
    }

    private RuntimeWiring buildRuntimeWiring(Connection dbConnection) {

        //try (ExecutorService executorService = Executors.newFixedThreadPool(8)) {

            return RuntimeWiring.newRuntimeWiring()
                //Wire Scalars
                .scalar(GraphQLScalarType.newScalar()
                        .name("Surface")
                        .description("It represents a surface: an amount and an unit (m² or km²)")
                        .coercing(new SurfaceCoercing() {
                        })
                        .build())
                //Wire Enums
                .type("Continent", builder -> builder.enumValues(new NaturalEnumValuesProvider<>(Continent.class)))

                //Wire type resolvers for unions and interfaces
                .type("Place", builder -> builder.typeResolver(new PojoClassTypeResolver()))

                //Wire Data Fetchers
                //.type("Query", builder -> builder.dataFetcher("countries", async(new DBCountriesDataFetcher(dbConnection), executorService)))
                .type("Query", builder -> builder.dataFetcher("countries", new AsyncDBCountriesDataFetcher(dbConnection)))
                .type("Query", builder -> builder.dataFetcher("country", async(new DBOneCountryDataFetcher(dbConnection))))
                .type("Query", builder -> builder.dataFetcher("provinces", async(new DBProvincesOfCountryDataFetcher(dbConnection))))
                .type("Query",  builder -> builder.dataFetcher("places", async(new DBPlacesDataFetcher(dbConnection))))
                .type("Country",  builder -> builder.dataFetcher("capital", async(new DBCityDataFetcher<>(dbConnection,Country::capital))))
                .type("Country",  builder -> builder.dataFetcher("provinces", async(new DBProvincesOfCountryDataFetcher(dbConnection))))
                .type("Province", builder -> builder.dataFetcher("capital", async(new DBCityDataFetcher<>(dbConnection,Province::capital))))
                .type("City",  builder -> builder
                        .dataFetcher("province", async(new DBProvinceFromCapitalDataFetcher(dbConnection)))
                        /* //wire old fields for backwards compatibility
                        .dataFetcher("latitude", env -> {
                            City city = env.getSource();
                            return Optional.ofNullable(city).map(City::geoLocation).map(GeoCoord::latitude).orElse(null);
                        })
                        .dataFetcher("longitude", env -> {
                            City city = env.getSource();
                            return Optional.ofNullable(city).map(City::geoLocation).map(GeoCoord::longitude).orElse(null);
                        })
                        */
                )
                .build();
        //} try
    }

    public ExecutionResult execute(String query) {
        return execute(query,null,null, Set.of());
    }

    public ExecutionResult execute(String query, Map<String, Object> variables, String operationName, Set<String> permissions) {
        ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput()
                .graphQLContext(Map.of("permissions", permissions))
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

