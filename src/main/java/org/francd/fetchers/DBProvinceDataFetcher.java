package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.City;
import org.francd.model.Province;

import java.sql.Connection;
import java.sql.ResultSet;

public class DBProvinceDataFetcher implements DataFetcher<Province>  {

    private final static  String PROVINCE_SQL = """
                SELECT *
                FROM province p
                WHERE
                    p.capital = ?
                """;

    private final Connection dbConnection;

    public DBProvinceDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Province get(DataFetchingEnvironment environment) throws Exception {

        City capital = environment.getSource();
        var statement = dbConnection.prepareStatement(PROVINCE_SQL);
        statement.setString(1, capital.name());

        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return Mapping.provinceOf(result);
        }
        return null;
    }

}
