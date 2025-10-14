package org.francd;

import graphql.ExecutionResult;
import graphql.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLRuntimeTest {

    private final GraphQLRuntime runtime;

    GraphQLRuntimeTest() throws IOException, SQLException {

        // Setup DB
        Connection dbConnection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/mondial",
                "postgres",
                "postgres789"
        );

        runtime = new GraphQLRuntime(dbConnection);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void validQuery() {
        // Given
        var query = """
            {
                countries(continent: "Europe") {
                    name
                    population
                    capital {
                        name
                        population
                        province {
                            name
                            population
                        }
                    }
                }
            }
        """;

        // When
        ExecutionResult result = runtime.execute(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isEmpty();

        //var resultData = result.getData();    //It returns directly a map with the countries
        var output = result.toSpecification();  //It returns a Map<String,Object> with 3 maps: data, errors, and extensions
        assertThat(output).isNotNull();         //(errors and extensions when present in execution result, of course)
        var data = (Map<String,Object>)output.get("data");  //this way, we have to get first a map with name "data"
        assertThat(data).isNotNull();
        assertThat(data).containsKey("countries").extracting("countries").isInstanceOf(List.class);
        var countries = (List<Map<String,Object>>)data.get("countries");
        assertThat(countries).hasSize(55);

        assertThat(countries).contains(Map.of(
                "name", "Switzerland",
                "population", 8670300,
                "capital", Map.of(
                        "name", "Bern",
                        "population", 134794,
                        "province", Map.of(
                                "name", "Bern",
                                "population", 1043132)
                ))
        );
    }

    @Test
    void invalidQuery() {
        // Given
        var query = """
            {
                countries {
                    latitude  # does not exist in schema
                    name
                    population
                    capital {
                        name
                        population
                    }
                }
            }
        """;

        // When
        ExecutionResult result = runtime.execute(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().getFirst()).isInstanceOf(ValidationError.class);
    }
}