package org.francd.db;

import org.francd.model.City;
import org.francd.model.Country;
import org.francd.model.GeoCoord;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Mapping {

    public static Country countryOf(ResultSet result) throws SQLException {
        return new Country(
                result.getString("code"),
                result.getString("name"),
                result.getInt("population"),
                result.getInt("area"),
                result.getString("capital")
        );
    }

    public static City cityOf(ResultSet result) throws SQLException {
        GeoCoord geoCoord = null;
        if (result.getObject("latitude") != null &&
                result.getObject("longitude") != null) {
            geoCoord = new GeoCoord(result.getDouble("latitude"),
                    result.getDouble("longitude"));
        }

        return new City(
                result.getString("name"),
                result.getInt("population"),
                geoCoord,
                result.getInt("elevation"),
                result.getString("province")
        );
    }
}
