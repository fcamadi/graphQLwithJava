package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.Mapping;
import org.francd.model.City;
import org.francd.model.Province;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;

public class DBProvinceDataFetcher implements DataFetcher<Province>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBProvinceDataFetcher.class);

    private final static  String PROVINCE_SQL = """
                SELECT *
                FROM province
                WHERE name = ?
                """;

    private final Connection dbConnection;

    public DBProvinceDataFetcher(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Province get(DataFetchingEnvironment environment) throws Exception {

        City capital = environment.getSource();
        String provinceName = capital.province();
        var statement = dbConnection.prepareStatement(PROVINCE_SQL);
        statement.setString(1, provinceName);

        LOGGER.info(PROVINCE_SQL.replaceAll("[\\s\\n]+", " "));

        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return Mapping.provinceOf(result);
        }
        return null;
    }

}
