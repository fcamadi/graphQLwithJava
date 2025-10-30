package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.francd.model.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BatchCityDataFetcher<T> implements DataFetcher<CompletableFuture<City>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchCityDataFetcher.class);

    private final Function<T, String> cityNameExtractor;

    public BatchCityDataFetcher(Function<T, String> cityNameExtractor) {
        this.cityNameExtractor = cityNameExtractor;
    }

    @Override
    public CompletableFuture<City> get(DataFetchingEnvironment environment) throws Exception {
        var capitalName = cityNameExtractor.apply(environment.getSource());
        DataLoader<String, City> dataLoader = environment.getDataLoader("City");

        LOGGER.info("dataLoader: city [{}]", capitalName);

        return dataLoader.load(capitalName);
    }
}
