package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.francd.model.Country;

import java.util.List;

public class StaticCountriesDataFetcher implements DataFetcher<List<Country>> {

    private final static List<Country> COUNTRIES = List.of(
            new Country("F", "France", 64300821, 547030, "Paris"),
            new Country("E", "Spain", 47400798, 504750, "Madrid"),
            new Country("CH", "Switzerland", 8670300, 41290, "Bern"),
            new Country("PT", "Portugal", 10300000, 92212, "Lisbon")
    );

    @Override
    public List<Country> get(DataFetchingEnvironment environment) {
        return COUNTRIES;
    }
}

