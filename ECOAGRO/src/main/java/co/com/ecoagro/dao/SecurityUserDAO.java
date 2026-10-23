package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// DAO para la supervisión y gestión de estados de los usuarios de EcoAgro.
public class SecurityUserDAO {

    // Lista compradores y vendedores con filtros de búsqueda, tipo y estado.
    public JsonArray listarUsuarios(String busqueda, String tipo, String estado, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT u.id_user,
                   u.name,
                   u.email,
                   u.number,
                   u.registration_date,
                   u.account_status,
                   u.status_reason,
                   u.status_updated_at,
                   CASE WHEN p.id_producer IS NOT NULL THEN 'VENDEDOR' ELSE 'COMPRADOR' END AS user_type,
                   sr.role_name AS staff_role
            FROM user u
            LEFT JOIN producer p ON p.id_user = u.id_user
            LEFT JOIN staff_assignment sa ON sa.id_user = u.id_user
            LEFT JOIN staff_role sr ON sr.id_role = sa.id_role
            WHERE 1=1
            """);

        List<Object> parametros = new ArrayList<>();

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append(" AND (u.name LIKE ? OR u.email LIKE ? OR u.number LIKE ?)");
            String patron = "%" + busqueda.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }

        if (tipo != null && !tipo.trim().isEmpty() && !"ALL".equalsIgnoreCase(tipo)) {
            if ("VENDEDOR".equalsIgnoreCase(tipo)) {
                sql.append(" AND p.id_producer IS NOT NULL");
            } else if ("COMPRADOR".equalsIgnoreCase(tipo)) {
                sql.append(" AND p.id_producer IS NULL");
            } else if ("STAFF".equalsIgnoreCase(tipo)) {
                sql.append(" AND sa.id_assignment IS NOT NULL");
            }
        }

        if (estado != null && !estado.trim().isEmpty() && !"ALL".equalsIgnoreCase(estado)) {
            sql.append(" AND u.account_status = ?");
            parametros.add(estado.trim().toUpperCase());
        }

        sql.append(" ORDER BY u.id_user DESC LIMIT ? OFFSET ?");
        int lim = (limit > 0 && limit <= 100) ? limit : 20;
        int off = Math.max(offset, 0);
        parametros.add(lim);
        parametros.add(off);

        JsonArray lista = new JsonArray();

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametros.size(); i++) {
                    sentencia.setObject(i + 1, parametros.get(i));
                }

                try (ResultSet rs = sentencia.executeQuery()) {
                    while (rs.next()) {
                        JsonObject u = new JsonObject();
                        u.addProperty("idUser", rs.getInt("id_user"));
                        u.addProperty("nombre", rs.getString("name"));
                        u.addProperty("email", rs.getString("email"));
                        u.addProperty("telefono", rs.getString("number"));
                        u.addProperty("fechaRegistro", rs.getString("registration_date"));
                        u.addProperty("estado", rs.getString("account_status"));
                        u.addProperty("motivoEstado", rs.getString("status_reason"));
                        u.addProperty("fechaEstado", rs.getString("status_updated_at"));
                        u.addProperty("tipoUsuario", rs.getString("user_type"));
                        u.addProperty("rangoStaff", rs.getString("staff_role"));
                        lista.add(u);
                    }
                }
            }
        }

        return lista;
    }

    // Consulta el detalle completo de un usuario sin exponer contraseñas.
    public JsonObject obtenerDetalleUsuario(int idUser) throws SQLException {
        String sql = """
            SELECT u.id_user,
                   u.name,
                   u.email,
                   u.number,
                   u.registration_date,
                   u.account_status,
                   u.status_reason,
                   u.status_updated_at,
                   p.id_producer,
                   p.phone_producer,
                   sr.role_name AS staff_role
            FROM user u
            LEFT JOIN producer p ON p.id_user = u.id_user
            LEFT JOIN staff_assignment sa ON sa.id_user = u.id_user
            LEFT JOIN staff_role sr ON sr.id_role = sa.id_role
            WHERE u.id_user = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);

                try (ResultSet rs = sentencia.executeQuery()) {
                    if (rs.next()) {
                        JsonObject detalle = new JsonObject();
                        detalle.addProperty("idUser", rs.getInt("id_user"));
                        detalle.addProperty("nombre", rs.getString("name"));
                        detalle.addProperty("email", rs.getString("email"));
                        detalle.addProperty("telefono", rs.getString("number"));
                        detalle.addProperty("fechaRegistro", rs.getString("registration_date"));
                        detalle.addProperty("estado", rs.getString("account_status"));
                        detalle.addProperty("motivoEstado", rs.getString("status_reason"));
                        detalle.addProperty("fechaEstado", rs.getString("status_updated_at"));

                        int idProd = rs.getInt("id_producer");
                        if (!rs.wasNull()) {
                            detalle.addProperty("tipoUsuario", "VENDEDOR");
                            detalle.addProperty("idProducer", idProd);
                            detalle.addProperty("telefonoProductor", rs.getString("phone_producer"));
                        } else {
                            detalle.addProperty("tipoUsuario", "COMPRADOR");
                        }

                        detalle.addProperty("rangoStaff", rs.getString("staff_role"));
                        return detalle;
                    }
                }
            }
        }

        return null;
    }

    // Actualiza el estado de la cuenta (ACTIVE o SUSPENDED).
    public boolean cambiarEstadoCuenta(int idUser, String nuevoEstado, String motivo) throws SQLException {
        String sql = """
            UPDATE user
            SET account_status = ?,
                status_reason = ?,
                status_updated_at = CURRENT_TIMESTAMP
            WHERE id_user = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, nuevoEstado);
                sentencia.setString(2, motivo);
                sentencia.setInt(3, idUser);

                return sentencia.executeUpdate() > 0;
            }
        }
    }

    // Consulta el estado actual de la cuenta de un usuario.
    public String obtenerEstadoCuenta(int idUser) throws SQLException {
        String sql = "SELECT account_status FROM user WHERE id_user = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);

                try (ResultSet rs = sentencia.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("account_status");
                    }
                }
            }
        }

        return null;
    }

    // Comprueba si existe un usuario con el ID especificado.
    public boolean existeUsuario(int idUser) throws SQLException {
        String sql = "SELECT 1 FROM user WHERE id_user = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);

                try (ResultSet rs = sentencia.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }
}
