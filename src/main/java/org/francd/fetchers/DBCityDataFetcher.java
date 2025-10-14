package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.City;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class DBCityDataFetcher<T> implements DataFetcher<City> {

    private final static String CITY_SQL = """
                SELECT * FROM city WHERE name = ?
                """;
    private final Connection dbConnection;
    private final Function<T, String> cityNameExtractor;

    public DBCityDataFetcher(Connection dbConnection, Function<T, String> cityNameExtractor) {
        this.cityNameExtractor = cityNameExtractor;
        this.dbConnection = dbConnection;
    }

    @Override
    public City get(DataFetchingEnvironment environment) throws SQLException {
        var capitalName = cityNameExtractor.apply(environment.getSource());

        var statement = dbConnection.prepareStatement(CITY_SQL);
        statement.setString(1, capitalName);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return Mapping.cityOf(result);
        }
        return null;
    }

}

