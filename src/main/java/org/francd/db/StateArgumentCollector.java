package org.francd.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class StateArgumentCollector {

    private final List<Object> arguments = new LinkedList<>();

    public void addString(String stringValue) {
        assert stringValue != null;
        arguments.add(stringValue);
    }

    public void addInt(Integer intValue) {
        assert intValue != null;
        arguments.add(intValue);
    }

    public void applyTo(PreparedStatement statement) throws SQLException {
        for(int i=0; i<arguments.size(); i++) {
            if (arguments.get(i) instanceof String stringValue) {
                statement.setString(i+1, stringValue);
            } else if (arguments.get(i) instanceof Integer intValue) {
                statement.setInt(i+1, intValue);
            } else {
                throw new RuntimeException("This should never happen!!!");
            }
        }
    }
}
