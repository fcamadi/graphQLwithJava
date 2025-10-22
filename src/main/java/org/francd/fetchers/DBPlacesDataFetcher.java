package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.Place;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBPlacesDataFetcher implements DataFetcher<List<Place>>  {

    private final Connection dbConnection;

    public DBPlacesDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public List<Place> get(DataFetchingEnvironment environment) throws Exception {

        Map<String, Object> criteria = environment.getArgument("criteria");

        List<Place> mappedResults = new ArrayList<>();
        try (var statement = queryWithCriteria(criteria)) {
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                var type = results.getString("type");
                Place place = switch (type) {
                    case "Country" -> Mapping.countryOf(results);
                    case "Province" -> Mapping.provinceOf(results);
                    case "City" -> Mapping.cityOf(results);
                    default -> throw new RuntimeException("What are you doing man?! What type is that? %s".formatted(type));
                };
                mappedResults.add(place);
            }
            return mappedResults;
        }
    }

    private PreparedStatement queryWithCriteria(Map<String, Object> criteria) throws SQLException {

        String unionQuery = """
            WITH place AS (
                    SELECT 'City' as type, null as code, name, province, elevation, latitude, longitude, null as area, null as capital, population
                    FROM city c
                UNION
                    SELECT 'Province' as type, null as code, name, null as province, null as elevation, null as latitude, null as longitude, area, capital, population
                    FROM province
                UNION
                    SELECT 'Country' as type, code, name, null as province, null as elevation, null as latitude, null as longitude, area, capital, population
                    FROM country
             )
             SELECT *
             FROM place
                WHERE name like ?
        """;

        String namePattern = (String)criteria.get("name");
        var statement = dbConnection.prepareStatement(unionQuery);
        statement.setString(1, namePattern.replaceAll("%","%%").replaceAll("[*]","%"));
        return statement;
    }
}
