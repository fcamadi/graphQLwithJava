package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.db.StateArgumentCollector;
import org.francd.model.Continent;
import org.francd.model.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBCountriesDataFetcher implements DataFetcher<List<Country>>  {

    private final static  String COUNTRY_SQL = """
                SELECT *
                FROM country c, encompasses e
                WHERE
                    e.continent = ?
                    AND c.code = e.country
                """;

    private final Connection dbConnection;

    public DBCountriesDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public List<Country> get(DataFetchingEnvironment environment) throws Exception {

        Map<String, Object> criteria = environment.getArgument("criteria");

        try (var statement = queryWithCriteria(criteria)) {
            ResultSet results = statement.executeQuery();
            List<Country> mappedResults = new ArrayList<>();
            while (results.next()) {
                Country country = Mapping.countryOf(results);
                mappedResults.add(country);
            }
            return mappedResults;
        }
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
