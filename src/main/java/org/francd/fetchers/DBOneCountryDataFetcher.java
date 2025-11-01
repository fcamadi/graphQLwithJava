package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.db.StateArgumentCollector;
import org.francd.model.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class DBOneCountryDataFetcher implements DataFetcher<Country>  {

    private final Connection dbConnection;

    public DBOneCountryDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Country get(DataFetchingEnvironment environment) throws Exception {

        Map<String, Object> criteria = environment.getArgument("criteria");
        if (Objects.isNull(criteria)) {
            throw new RuntimeException("Criteria cannot be null");
        }
        try (var statement = queryWithCriteria(criteria)) {
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return Mapping.countryOf(result);
            }
            return null;
        }
    }

    private PreparedStatement queryWithCriteria(Map<String, Object> criteria) throws SQLException {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("""
            SELECT *
            FROM country c
        """);

        StateArgumentCollector collector = new StateArgumentCollector();

        String country = criteria.get("country").toString();
        if (country != null) {
            stringBuilder.append(" WHERE name = ?");
            collector.addString(country);
        } else {
            throw new RuntimeException("You must select a country!");
        }

        var statement = dbConnection.prepareStatement(stringBuilder.toString());
        collector.applyTo(statement);
        return statement;
    }
}
