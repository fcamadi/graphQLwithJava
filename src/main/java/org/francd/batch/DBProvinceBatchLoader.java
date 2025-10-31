package org.francd.batch;

import org.dataloader.BatchLoader;
import org.francd.db.Mapping;
import org.francd.model.Province;
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

public class DBProvinceBatchLoader implements BatchLoader<String, Province> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBProvinceBatchLoader.class);

    private final Connection dbConnection;

    public DBProvinceBatchLoader(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public CompletionStage<List<Province>> load(List<String> keys) {

        if (keys.isEmpty()) {
            LOGGER.info("load - list of keys is empty");
            return CompletableFuture.completedFuture(List.of());
        }
        LOGGER.info("load - list of keys: {}", keys);

        StringBuilder stringBuilderSQL = new StringBuilder();
        stringBuilderSQL.append("""
            SELECT p.name, p.capital, p.area, p.population
            FROM province p
            WHERE p.name IN (%s)
        """);


        return CompletableFuture.supplyAsync((() -> {
            try {
                PreparedStatement statement = dbConnection.prepareStatement(stringBuilderSQL.toString().formatted(
             "'" + keys.stream().filter(Objects::nonNull)
                     .map(k -> k.replaceAll("'", "''"))
                     .collect(Collectors.joining("', '")) + "'"
                ));

                LOGGER.info(statement.toString().replaceAll("[\\s\\n]+", " "));

                ResultSet results = statement.executeQuery();
                //to return results in the same order, we need to do this
                Map<String, Province> mappedResults = new ConcurrentHashMap<>();
                while (results.next()) {
                    mappedResults.put(results.getString("name"), Mapping.provinceOf(results));
                }
                LOGGER.info("load - list of provinces - size: {}", mappedResults.size());
                return keys.stream().map(mappedResults::get).toList();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
