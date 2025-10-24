package org.francd.model;

import org.springframework.lang.NonNull;

public record City(
        @NonNull
        String name,
        Integer population,
        GeoCoord geoLocation,
        Integer elevation,
        @GraphqlType("Province")
        String province
) implements Place {}

