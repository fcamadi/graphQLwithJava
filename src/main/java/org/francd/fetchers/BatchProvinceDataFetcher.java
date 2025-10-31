package org.francd.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.francd.model.City;
import org.francd.model.Province;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class BatchProvinceDataFetcher implements DataFetcher<CompletableFuture<Province>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchProvinceDataFetcher.class);

    @Override
    public CompletableFuture<Province> get(DataFetchingEnvironment environment) throws Exception {
        City city = environment.getSource();
        DataLoader<String, Province> dataLoader = environment.getDataLoader("Province");

        LOGGER.info("dataLoader: province of city [{}]", city.name());

        return dataLoader.load(city.province());
    }
}
