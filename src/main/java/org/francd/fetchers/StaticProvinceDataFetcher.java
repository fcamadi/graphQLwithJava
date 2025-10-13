package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.model.City;
import org.francd.model.Province;

import java.util.Map;

public class StaticProvinceDataFetcher implements DataFetcher<Province> {

    private final static Map<String, Province> PROVINCES = Map.of(
            "Île-de-France", new Province("Île-de-France", 12082144, "Paris", 12345),
            "Madrid", new Province("Madrid", 6726640,  "Madrid", 12345),
            "Bern", new Province("Bern", 1043132,  "Bern", 2345),
            "Prov. Lisbon", new Province("Prov. Lisbon", 189545, "Lisbon", 2345)
    );

    @Override
    public Province get(DataFetchingEnvironment environment) {
        City citySource = environment.getSource();
        String provinceName = citySource.province();
        return PROVINCES.get(provinceName);
    }
}

