package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.db.StateArgumentCollector;
import org.francd.model.Country;
import org.francd.model.Province;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBProvincesOfCountryDataFetcher implements DataFetcher<List<Province>>  {

    private final static  String PROVINCES_SQL = """
            SELECT c.name as country, p."name", p.capital,p."area",p.population\s
            FROM country c, province p
                     WHERE
                         p.country = c.code
                         AND c.code = ?
            """;

    private final Connection dbConnection;

    public DBProvincesOfCountryDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Province> get(DataFetchingEnvironment environment) throws Exception {

        Map<String, Object> criteria = environment.getArgument("criteria");  // It comes null. Why???
        if (environment.getVariables().containsKey("criteria")) {
            criteria = (Map<String, Object>) environment.getVariables().get("criteria");
        }

        try (var statement = queryWithCriteria(criteria)) {
            ResultSet result = statement.executeQuery();
            List<Province> mappedResults = new ArrayList<>();
            while (result.next()) {
                Province province = Mapping.provinceOf(result);
                mappedResults.add(province);
            }
            return mappedResults;
        }
    }

    @SuppressWarnings("unchecked")
    private PreparedStatement queryWithCriteria(Map<String, Object> criteria) throws SQLException {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("""
            SELECT p.name, p.capital, p.area, p.population
            FROM country c
                INNER JOIN province p
                    ON p.country = c.code
        """);

        StateArgumentCollector collector = new StateArgumentCollector();

        String country = criteria.get("country").toString();
        if (country != null) {
            stringBuilder.append(" WHERE c.name = ?");
            collector.addString(country);
        } else {
            throw new RuntimeException("You must select a country!");
        }

        Map<String, Integer> populationRange = (Map<String, Integer>) criteria.get("populationRange");
        if (populationRange != null) {
            if (populationRange.containsKey("above")) {
                stringBuilder.append("  AND p.population >= ?");
                collector.addInt(populationRange.get("above"));
            }if (populationRange.containsKey("below")) {
                stringBuilder.append("  AND p.population <= ?");
                collector.addInt(populationRange.get("below"));
            }
        }

        var statement = dbConnection.prepareStatement(stringBuilder.toString());
        collector.applyTo(statement);
        return statement;
    }

}
