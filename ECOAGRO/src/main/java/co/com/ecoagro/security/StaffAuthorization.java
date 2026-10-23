package co.com.ecoagro.security;

import co.com.ecoagro.config.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Métodos de verificación de permisos administrativos.
public final class StaffAuthorization {

    private StaffAuthorization() {
    }

    // Comprueba si un usuario tiene un rango específico en staff_assignment.
    public static boolean tieneRango(
            int idUsuario,
            String rango
    ) throws SQLException {

        String sql = """
            SELECT 1
            FROM staff_assignment sa
            INNER JOIN staff_role sr
                ON sr.id_role = sa.id_role
            WHERE sa.id_user = ?
              AND sr.role_name = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUsuario);
                sentencia.setString(2, rango);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Comprueba si el usuario es SUPERADMINISTRADOR.
    public static boolean esSuperadministrador(int idUsuario) throws SQLException {
        return tieneRango(idUsuario, "SUPERADMINISTRADOR");
    }

    // Comprueba si el usuario es ADMIN_SEGURIDAD.
    public static boolean esAdministradorSeguridad(int idUsuario) throws SQLException {
        return tieneRango(idUsuario, "ADMIN_SEGURIDAD");
    }

    // Comprueba si el usuario tiene algún rol del personal administrativo.
    public static boolean esPersonalAutorizado(int idUsuario) throws SQLException {
        return esSuperadministrador(idUsuario) || esAdministradorSeguridad(idUsuario);
    }

    // Comprueba si un usuario pertenece al staff administrativo.
    public static boolean esMiembroStaff(int idUsuario) throws SQLException {
        String sql = "SELECT 1 FROM staff_assignment WHERE id_user = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUsuario);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Obtiene el nombre del rol del usuario o null si no es staff.
    public static String obtenerRango(int idUsuario) throws SQLException {
        String sql = """
            SELECT sr.role_name
            FROM staff_assignment sa
            INNER JOIN staff_role sr ON sr.id_role = sa.id_role
            WHERE sa.id_user = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUsuario);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (resultado.next()) {
                        return resultado.getString("role_name");
                    }
                }
            }
        }
        return null;
    }
}
