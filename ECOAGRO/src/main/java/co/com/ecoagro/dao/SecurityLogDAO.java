package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

// DAO para el registro y consulta de eventos de auditoría y seguridad.
public class SecurityLogDAO {

    // Registra un evento en la tabla security_log de forma segura.
    public void registrarEvento(
            Integer idUser,
            Integer actorId,
            String eventType,
            String description,
            String ipAddress
    ) {
        String sql = """
            INSERT INTO security_log (id_user, actor_id, event_type, description, ip_address)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                System.err.println("No se pudo conectar a la BD para registrar el evento: " + eventType);
                return;
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                if (idUser != null && idUser > 0) {
                    sentencia.setInt(1, idUser);
                } else {
                    sentencia.setNull(1, Types.INTEGER);
                }

                if (actorId != null && actorId > 0) {
                    sentencia.setInt(2, actorId);
                } else {
                    sentencia.setNull(2, Types.INTEGER);
                }

                sentencia.setString(3, eventType);
                sentencia.setString(4, description);
                sentencia.setString(5, ipAddress != null ? ipAddress : "127.0.0.1");

                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar evento de auditoría: " + e.getMessage());
        }
    }

    // Consulta el listado de eventos con filtros opcionales.
    public JsonArray listarEventos(Integer idUser, String eventType, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT sl.id_log,
                   sl.id_user,
                   u.name AS usuario_nombre,
                   u.email AS usuario_email,
                   sl.actor_id,
                   a.name AS actor_nombre,
                   sl.event_type,
                   sl.description,
                   sl.ip_address,
                   sl.created_at
            FROM security_log sl
            LEFT JOIN user u ON u.id_user = sl.id_user
            LEFT JOIN user a ON a.id_user = sl.actor_id
            WHERE 1=1
            """);

        if (idUser != null && idUser > 0) {
            sql.append(" AND sl.id_user = ?");
        }
        if (eventType != null && !eventType.trim().isEmpty() && !"ALL".equalsIgnoreCase(eventType)) {
            sql.append(" AND sl.event_type = ?");
        }

        sql.append(" ORDER BY sl.created_at DESC LIMIT ?");

        JsonArray lista = new JsonArray();

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
                int paramIndex = 1;

                if (idUser != null && idUser > 0) {
                    sentencia.setInt(paramIndex++, idUser);
                }
                if (eventType != null && !eventType.trim().isEmpty() && !"ALL".equalsIgnoreCase(eventType)) {
                    sentencia.setString(paramIndex++, eventType.trim());
                }

                int limiteSeguro = (limit > 0 && limit <= 200) ? limit : 50;
                sentencia.setInt(paramIndex, limiteSeguro);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        JsonObject fila = new JsonObject();
                        fila.addProperty("idLog", resultado.getInt("id_log"));

                        int idU = resultado.getInt("id_user");
                        if (!resultado.wasNull()) {
                            fila.addProperty("idUser", idU);
                            fila.addProperty("usuarioNombre", resultado.getString("usuario_nombre"));
                            fila.addProperty("usuarioEmail", resultado.getString("usuario_email"));
                        }

                        int idA = resultado.getInt("actor_id");
                        if (!resultado.wasNull()) {
                            fila.addProperty("actorId", idA);
                            fila.addProperty("actorNombre", resultado.getString("actor_nombre"));
                        }

                        fila.addProperty("eventType", resultado.getString("event_type"));
                        fila.addProperty("description", resultado.getString("description"));
                        fila.addProperty("ipAddress", resultado.getString("ip_address"));
                        fila.addProperty("createdAt", resultado.getString("created_at"));

                        lista.add(fila);
                    }
                }
            }
        }

        return lista;
    }

    // Obtiene las métricas generales del panel administrativo.
    public JsonObject obtenerResumenMetricas() throws SQLException {
        JsonObject metricas = new JsonObject();

        String sql = """
            SELECT
                (SELECT COUNT(*) FROM user) AS total_usuarios,
                (SELECT COUNT(*) FROM user WHERE account_status = 'ACTIVE') AS usuarios_activos,
                (SELECT COUNT(*) FROM user WHERE account_status = 'SUSPENDED') AS usuarios_suspendidos,
                (SELECT COUNT(*) FROM security_incident WHERE status = 'OPEN') AS incidentes_abiertos,
                (SELECT COUNT(*) FROM security_incident) AS total_incidentes,
                (SELECT COUNT(*) FROM security_log) AS total_eventos
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet rs = sentencia.executeQuery()) {
                if (rs.next()) {
                    metricas.addProperty("totalUsuarios", rs.getInt("total_usuarios"));
                    metricas.addProperty("usuariosActivos", rs.getInt("usuarios_activos"));
                    metricas.addProperty("usuariosSuspendidos", rs.getInt("usuarios_suspendidos"));
                    metricas.addProperty("incidentesAbiertos", rs.getInt("incidentes_abiertos"));
                    metricas.addProperty("totalIncidentes", rs.getInt("total_incidentes"));
                    metricas.addProperty("totalEventos", rs.getInt("total_eventos"));
                }
            }
        }

        return metricas;
    }
}
