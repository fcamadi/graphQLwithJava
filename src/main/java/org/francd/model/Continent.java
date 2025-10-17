package org.francd.model;

public enum Continent {
    Europe,
    Asia,
    Africa,
    SouthAmerica ("South America"),
    NorthAmerica ("North America"),
    Oceania("Australia/Oceania");

    private final String dbName;

    Continent(String dbName) {
        this.dbName = dbName;
    }

    Continent() {
        this.dbName = this.name();
    }

    public String dbName() {
        return this.dbName;
    }
}
