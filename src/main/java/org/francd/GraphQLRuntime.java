package org.francd;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.francd.batch.DBCityBatchLoader;
import org.francd.batch.DBProvinceBatchLoader;
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

public class GraphQLRuntime {

    private final GraphQL graphql;
    private final Connection dbConnection;

    public GraphQLRuntime(Connection dbConnection) throws IOException {

        this.dbConnection = dbConnection;

        // The SchemaGenerator is responsible for turning a type registry and wiring into an executable GraphQLSchema
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        // buildTypeDefinitionRegistry() reads the SDL files (e.g., *.graphqls) and
        // registers all object types, enums, inputs, etc., in a TypeDefinitionRegistry.
        TypeDefinitionRegistry typeRegistry = buildTypeDefinitionRegistry();
        // buildRuntimeWiring() maps each field in the schema to a DataFetcher – the Java code
        // that actually supplies the data when a query asks for that field.
        RuntimeWiring wiring = buildRuntimeWiring();
        // The generator combines the type definitions with the wiring, producing a GraphQLSchema
        // that knows both the shape of the API and how to fetch data.
        GraphQLSchema schema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);

        // To add more than one instrumentation class:
        ChainedInstrumentation chainedInstrumentations = new ChainedInstrumentation(
                new LoggingInstrumentation(),
                new DataFetcherCounterInstrumentation()
        );

        // This builds a GraphQL object that can later be called with execute(query)
        // to run queries against the schema.
        graphql = GraphQL.newGraphQL(schema)
                //.instrumentation(new LoggingInstrumentation())
                //.instrumentation(new AccessControlInstrumentation())
                //.instrumentation(new DataFetcherCounterInstrumentation())  // the last one is the only one applied
                //.instrumentation(chainedInstrumentations)
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
        //typeRegistry.add(TypeHelper.objectDefinitionOf(Province.class));
        // We add also the City
        //typeRegistry.add(TypeHelper.objectDefinitionOf(City.class));

        return typeRegistry;
    }

    private RuntimeWiring buildRuntimeWiring() {

        return RuntimeWiring.newRuntimeWiring()
                //Wire Scalars
                .scalar(GraphQLScalarType.newScalar()
                        .name("Surface")
                        .description("It represents a surface: an amount and an unit (m² or km²)")
                        .coercing(new SurfaceCoercing())
                        .build())
                //Wire Enums
                .type("Continent", builder -> builder.enumValues(new NaturalEnumValuesProvider<>(Continent.class)))

                //Wire type resolvers for unions and interfaces
                .type("Place", builder -> builder.typeResolver(new PojoClassTypeResolver()))

                //Wire Data Fetchers
                .type("Query", builder ->
                    builder
                        .dataFetcher("countries", new DBCountriesDataFetcher(dbConnection))
                        .dataFetcher("country", new DBOneCountryDataFetcher(dbConnection))
                        .dataFetcher("provinces", new DBProvincesOfCountryDataFetcher(dbConnection))
                        .dataFetcher("places", new DBPlacesDataFetcher(dbConnection)))

                //.type("Country",  builder -> builder.dataFetcher("capital", new DBCityDataFetcher<>(dbConnection,Country::capital)))
                .type("Country", builder ->
                    builder
                        .dataFetcher("capital", new BatchCityDataFetcher<>(Country::capital))
                        .dataFetcher("provinces", new DBProvincesOfCountryDataFetcher(dbConnection)))
                        //.dataFetcher("provinces", new DBProvinceDataFetcher(dbConnection)))

                //.type("Province", builder -> builder.dataFetcher("capital", new DBCityDataFetcher<>(dbConnection,Province::capital)))
                .type("Province", builder -> builder.dataFetcher("capital", new BatchCityDataFetcher<>(Province::capital)))
                .type("City",  builder -> builder
                                //.dataFetcher("province", new DBProvinceFromCapitalDataFetcher(dbConnection))
                                //.dataFetcher("province", new DBProvinceDataFetcher(dbConnection))
                                .dataFetcher("province", new BatchProvinceDataFetcher())
                )
                .build();
    }

    public ExecutionResult execute(String query) {
        return execute(query,null,null, Set.of());
    }

    public ExecutionResult execute(String query, Map<String, Object> variables, String operationName, Set<String> permissions) {

        var cityDataLoader = DataLoaderFactory.newDataLoader(
            new DBCityBatchLoader(dbConnection),
                DataLoaderOptions.newOptions().setMaxBatchSize(10).setCachingEnabled(true)
        );
        var provinceDataLoader = DataLoaderFactory.newDataLoader(
                new DBProvinceBatchLoader(dbConnection),
                DataLoaderOptions.newOptions().setMaxBatchSize(10).setCachingEnabled(true)
        );
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry
                .register("City", cityDataLoader)
                .register("Province", provinceDataLoader);

        ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput()
                .graphQLContext(Map.of("permissions", permissions))
                .dataLoaderRegistry(dataLoaderRegistry)
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

