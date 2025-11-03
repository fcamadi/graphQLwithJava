package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.db.StateArgumentCollector;
import org.francd.model.Continent;
import org.francd.model.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncDBCountriesDataFetcher implements DataFetcher<CompletableFuture<List<Country>>>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDBCountriesDataFetcher.class);

        private final Connection dbConnection;

        public AsyncDBCountriesDataFetcher(Connection dbConnection) {
            this.dbConnection = dbConnection;
        }

        @Override
        public CompletableFuture<List<Country>> get(DataFetchingEnvironment environment) throws Exception {

            LOGGER.info("Start get");

            Map<String, Object> criteria = environment.getArgument("criteria");

            return CompletableFuture.supplyAsync( () -> {
                try (var statement = queryWithCriteria(criteria)) {
                    ResultSet results = statement.executeQuery();
                    List<Country> mappedResults = new ArrayList<>();
                    while (results.next()) {
                        Country country = Mapping.countryOf(results);
                        mappedResults.add(country);
                    }
                    LOGGER.info("End get - mappedResults size {}", mappedResults.size());
                    return mappedResults;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        private PreparedStatement queryWithCriteria(Map<String, Object> criteria) throws SQLException {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("""
            SELECT *
                FROM country c, encompasses e
                WHERE
                    c.code = e.country
            """);

            StateArgumentCollector collector = new StateArgumentCollector();

            Continent continent = (Continent)criteria.get("continent");
            if (continent != null) {
                stringBuilder.append(" AND e.continent = ?");
                collector.addString(continent.dbName());
            }
            @SuppressWarnings("unchecked")
            Map<String, Integer> populationRange = (Map<String, Integer>) criteria.get("populationRange");
            if (populationRange != null) {
                if (populationRange.containsKey("above")) {
                    stringBuilder.append("  AND c.population >= ?");
                    collector.addInt(populationRange.get("above"));
                }if (populationRange.containsKey("below")) {
                    stringBuilder.append("  AND c.population <= ?");
                    collector.addInt(populationRange.get("below"));
                }
            }

            var statement = dbConnection.prepareStatement(stringBuilder.toString());
            collector.applyTo(statement);
            return statement;
        }
    }

