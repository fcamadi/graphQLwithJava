package org.francd.model;

public record Country(
        String code,
        String name,
        Integer population,
        Integer area,
        String capital
) implements  Place  {}

