package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;
import co.com.ecoagro.security.PasswordService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Consulta las credenciales almacenadas para el inicio de sesión. */
public class LoginDAO {

    /** Busca la cuenta por correo o celular y verifica la contraseña con el hash almacenado. */
    public boolean autenticar(String identifier, String password)
            throws SQLException {

        String sql = """
                SELECT password
                FROM user
                WHERE email = ? OR number = ?
                LIMIT 1
                """;

        String hash;
        try (Connection conn = ConexionDB.obtenerConexion()) {

            if (conn == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, identifier);
                ps.setString(2, identifier);

                try (ResultSet rs = ps.executeQuery()) {
                    hash = rs.next() ? rs.getString("password") : null;
                }
            }
        }
        // La verificación se realiza después de liberar la conexión a MySQL.
        return PasswordService.verificar(password, hash);
    }
}
