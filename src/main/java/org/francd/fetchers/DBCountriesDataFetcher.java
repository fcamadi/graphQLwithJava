package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.db.StateArgumentCollector;
import org.francd.model.Continent;
import org.francd.model.Country;

import java.io.StringWriter;
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

    @SuppressWarnings("unchecked")
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

        StringWriter stringWriter = new StringWriter();
        stringWriter.append("""
            SELECT *
                FROM country c, encompasses e
                WHERE
                    c.code = e.country
        """);

        StateArgumentCollector collector = new StateArgumentCollector();

        ArrayList<String> criteriaClauses = new ArrayList<>();
        Continent continent = (Continent)criteria.get("continent");
        if (continent != null) {
            stringWriter.append(" AND e.continent = ?");
            collector.addString(continent.dbName());
        }
        Map<String, Integer> populationRange = (Map<String, Integer>) criteria.get("populationRange");
        if (populationRange != null) {
            if (populationRange.containsKey("above")) {
                stringWriter.append("  AND c.population >= ?");
                collector.addInt(populationRange.get("above"));
            }if (populationRange.containsKey("below")) {
                stringWriter.append("  AND c.population <= ?");
                collector.addInt(populationRange.get("below"));
            }
        }

        var statement = dbConnection.prepareStatement(stringWriter.toString());
        collector.applyTo(statement);
        return statement;
    }
}
