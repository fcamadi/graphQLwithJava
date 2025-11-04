package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.db.ProvinceDao;
import org.francd.model.Province;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Map;

import static org.francd.db.ProvinceDao.findByName;

public class DBUpdateProvinceDataFetcher implements DataFetcher<Province> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUpdateProvinceDataFetcher.class);

    private final Connection connection;

    public DBUpdateProvinceDataFetcher(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Province get(DataFetchingEnvironment env) throws Exception {
        //arguments supplied by the mutation
        Map<String, Object> input = env.getArgument("input");    // ProvinceInput map

        String name;
        if (input.containsKey("name")) {
            name = input.get("name").toString();
        } else {
            LOGGER.error("name field missing!!!");
            throw new RuntimeException("name field missing!!!");
        }

        //fetch the existing province
        Province existing = findByName(connection, name)
                .orElseThrow(() -> new IllegalArgumentException("Province '" + name + "' not found"));
        LOGGER.info("Province found: {}", existing.name());

        //modify province
        Province updated = new Province(
                existing.name(),
                input.containsKey("population") ?  (Integer) input.get("population") : existing.population(),
                existing.capital(),
                input.containsKey("area") ?  (Integer) input.get("area") : existing.area()
        );

        //persist the changes
        ProvinceDao.update(connection, updated);

        LOGGER.info("Province updated: {}", updated);
        //return the updated object
        return updated;
    }
}
