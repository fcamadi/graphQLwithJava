package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.Country;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

        String  continent = environment.getArgument("continent");

        var statement = dbConnection.prepareStatement(COUNTRY_SQL);
        statement.setString(1, continent);
        ResultSet results = statement.executeQuery();

        List<Country> mappedResults = new ArrayList<>();
        while (results.next()) {
            Country country = Mapping.countryOf(results);
            mappedResults.add(country);
        }
        return mappedResults;
    }
}
