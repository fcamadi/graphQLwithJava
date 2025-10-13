package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.model.City;
import org.francd.model.Country;
import org.francd.model.GeoCoord;

import java.util.Map;

public class StaticCityDataFetcher implements DataFetcher<City> {
    private final static Map<String, City> CITIES = Map.of(
            "Paris", new City("Paris", 2249975, new GeoCoord(48.86,2.35), 28, "Île-de-France"),
            "Madrid", new City("Madrid", 3277451, new GeoCoord(40.38,	-3.72), 667, "Madrid"),
            "Bern", new City("Bern", 134794, new GeoCoord(46.95,	7.45), 542, "Bern"),
            "Lisbon", new City("Lisbon", 575739, new GeoCoord(38.7,	-9.1), 7, "District of Columbia")
    );

    @Override
    public City get(DataFetchingEnvironment environment) {
        Country country = environment.getSource();
        var capitalName = country.capital();
        return CITIES.get(capitalName);
    }

}

