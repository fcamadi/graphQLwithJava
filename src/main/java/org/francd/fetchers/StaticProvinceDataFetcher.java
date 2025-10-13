package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.model.Province;

import java.util.Map;
import java.util.function.Function;

public class StaticProvinceDataFetcher<T> implements DataFetcher<Province> {

    private final static Map<String, Province> PROVINCES = Map.of(
            "Paris", new Province("Île-de-France", 12082144, "Paris", 12345),
            "Madrid", new Province("Madrid", 6726640,  "Madrid", 12345),
            "Bern", new Province("Bern", 1043132,  "Bern", 2345),
            "Lisbon", new Province("Prov. Lisbon", 189545, "Lisbon", 2345)
    );

    private final Function<T, String> cityNameExtractor;

    public StaticProvinceDataFetcher(Function<T, String> cityNameExtractor) {
        this.cityNameExtractor = cityNameExtractor;
    }

    @Override
    public Province get(DataFetchingEnvironment environment) {
        var capitalName = cityNameExtractor.apply(environment.getSource());
        return PROVINCES.get(capitalName);
    }
}

