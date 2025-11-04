package org.francd.db;

import org.francd.model.Province;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class ProvinceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvinceDao.class);

    public static Optional<Province> findByName(Connection conn, String name) throws SQLException {
        String sql = """
                SELECT name, population, capital, area
                   FROM province
                   WHERE name = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(Mapping.provinceOf(rs));
            }
        }
    }

    public static void update(Connection conn, Province province) throws SQLException {
        String sql = """
            UPDATE province
                SET population = ?, capital = ?, area = ?
                WHERE name = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, province.population());
            ps.setString(2, province.capital());
            setNullableInt(ps, 3, province.area());
            ps.setString(4, province.name());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                LOGGER.error("No province updated – name not found: {} ", province.name());
                throw new SQLException("No province updated – name not found: " + province.name());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, v);
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }
}
