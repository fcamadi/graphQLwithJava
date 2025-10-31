package org.francd.batch;

import org.dataloader.BatchLoader;
import org.francd.db.Mapping;
import org.francd.model.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DBCityBatchLoader implements BatchLoader<String, City> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBCityBatchLoader.class);

    private final static String CITY_SQL = """
                SELECT * FROM city WHERE name IN (%s)
                """;

    private final Connection dbConnection;

    public DBCityBatchLoader(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public CompletionStage<List<City>> load(List<String> keys) {

        if (keys.isEmpty()) {
            LOGGER.info("load - list of keys is empty");
            return CompletableFuture.completedFuture(List.of());
        }
        LOGGER.info("load - list of keys: {}", keys);

        return CompletableFuture.supplyAsync((() -> {
            try {
                PreparedStatement statement = dbConnection.prepareStatement(CITY_SQL.formatted(
             "'" + keys.stream().filter(Objects::nonNull)
                     .map(k -> k.replaceAll("'", "''"))
                     .collect(Collectors.joining("', '")) + "'"
                ));

                LOGGER.info(statement.toString().replaceAll("[\\s\\n]+", " "));

                ResultSet results = statement.executeQuery();
                //to return results in the same order, we need to do this
                Map<String, City> mappedResults = new ConcurrentHashMap<>();
                while (results.next()) {
                    mappedResults.put(results.getString("name"), Mapping.cityOf(results));
                }

                LOGGER.info("load - list of cities - size: {}", mappedResults.size());

                //this is what guarantees the right order: we get the order in the keys,
                //so we use the keys to return the results back:
                return keys.stream().map(mappedResults::get).toList();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
