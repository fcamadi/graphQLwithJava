package org.francd;

import graphql.ExecutionResult;
import graphql.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLRuntimeTest {

    private final GraphQLRuntime runtime;

    GraphQLRuntimeTest() throws IOException {
        runtime = new GraphQLRuntime();
    }

    @Test
    public void validQuery() {
        // Given
        var query = """
            {
                countries {
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
        assertThat(result.getErrors()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validQueryProvince() {
        // Given
        var query = """
            {
                countries {
                    name
                    population
                    capital {
                        name
                    }
                    provinces {
                        name
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
        assertThat(output).isNotNull();
        var data = (Map<String,Object>)output.get("data");  //this way, we have to get first a map with name "data"
        assertThat(data).isNotNull();
        assertThat(data).containsKey("countries").extracting("countries").isInstanceOf(List.class);
        var countries = (List<Map<String,Object>>)data.get("countries");
        assertThat(countries).hasSize(4);
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