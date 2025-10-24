package org.francd.model;

import org.springframework.lang.NonNull;

public record Province(
        @NonNull
        String name,
        Integer population,
        @GraphqlType("City")
        String capital,
        Integer area
) implements  Place {}
