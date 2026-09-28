package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    public boolean autenticar(String identifier, String password)
            throws SQLException {

        String sql = """
                SELECT 1
                FROM user
                WHERE (email = ? OR number = ?)
                  AND password = ?
                LIMIT 1
                """;

        try (Connection conn = ConexionDB.obtenerConexion()) {

            if (conn == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, identifier);
                ps.setString(2, identifier);
                ps.setString(3, password);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }
}