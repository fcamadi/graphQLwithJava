package org.francd.model;

public record City(
        String name,
        Integer population,
        GeoCoord geoLocation,
        Integer elevation,
        String province
) {}

