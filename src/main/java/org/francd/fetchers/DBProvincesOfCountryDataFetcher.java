package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.Country;
import org.francd.model.Province;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Province> get(DataFetchingEnvironment environment) throws Exception {

        Country country = environment.getSource();
        var statement = dbConnection.prepareStatement(PROVINCES_SQL);
        statement.setString(1, country.code());

        ResultSet result = statement.executeQuery();
        List<Province> mappedResults = new ArrayList<>();
        while (result.next()) {
            Province province = Mapping.provinceOf(result);
            mappedResults.add(province);
        }
        return mappedResults;
    }

}
