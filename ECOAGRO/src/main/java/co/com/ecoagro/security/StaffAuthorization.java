
package co.com.ecoagro.security;

import co.com.ecoagro.config.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class StaffAuthorization {

    private StaffAuthorization() {
        // Esta clase solo ofrece metodos de autorizacion
    }

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

        try (
            Connection conexion = ConexionDB.obtenerConexion()
        ) {
            if (conexion == null) {
                throw new SQLException(
                    "No fue posible conectar con la base de datos."
                );
            }

            try (
                PreparedStatement sentencia =
                    conexion.prepareStatement(sql)
            ) {
                sentencia.setInt(1, idUsuario);
                sentencia.setString(2, rango);

                try (
                    ResultSet resultado = sentencia.executeQuery()
                ) {
                    return resultado.next();
                }
            }
        }
    }

    public static boolean esSuperadministrador(
            int idUsuario
    ) throws SQLException {

        return tieneRango(
            idUsuario,
            "SUPERADMINISTRADOR"
        );
    }

    public static boolean esAdministradorSeguridad(
            int idUsuario
    ) throws SQLException {

        return tieneRango(
            idUsuario,
            "ADMIN_SEGURIDAD"
        );
    }
}
