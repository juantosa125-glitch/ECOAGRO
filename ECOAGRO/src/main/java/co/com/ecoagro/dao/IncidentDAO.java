package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// DAO para la gestión y seguimiento de incidentes de seguridad.
public class IncidentDAO {

    // Registra un nuevo incidente y su primer registro en el historial.
    public int crearIncidente(
            int idUserAffected,
            int reportedBy,
            String title,
            String description,
            String category,
            String priority
    ) throws SQLException {
        String sqlIncidente = """
            INSERT INTO security_incident
                (id_user_affected, reported_by, title, description, category, priority, status)
            VALUES (?, ?, ?, ?, ?, ?, 'OPEN')
            """;

        String sqlHistorial = """
            INSERT INTO incident_history
                (id_incident, actor_id, previous_status, new_status, comment)
            VALUES (?, ?, NULL, 'OPEN', ?)
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            conexion.setAutoCommit(false);

            int idGenerado = -1;

            try (PreparedStatement psInc = conexion.prepareStatement(sqlIncidente, Statement.RETURN_GENERATED_KEYS)) {
                psInc.setInt(1, idUserAffected);
                psInc.setInt(2, reportedBy);
                psInc.setString(3, title);
                psInc.setString(4, description);
                psInc.setString(5, category);
                psInc.setString(6, priority != null ? priority : "MEDIA");

                psInc.executeUpdate();

                try (ResultSet rsKeys = psInc.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        idGenerado = rsKeys.getInt(1);
                    }
                }

                if (idGenerado > 0) {
                    try (PreparedStatement psHist = conexion.prepareStatement(sqlHistorial)) {
                        psHist.setInt(1, idGenerado);
                        psHist.setInt(2, reportedBy);
                        psHist.setString(3, "Creación del incidente.");
                        psHist.executeUpdate();
                    }
                }

                conexion.commit();
                return idGenerado;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    // Consulta la lista de incidentes con filtros opcionales.
    public JsonArray listarIncidentes(Integer idUser, String status, String category, String priority) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT si.id_incident,
                   si.id_user_affected,
                   ua.name AS usuario_afectado_nombre,
                   ua.email AS usuario_afectado_email,
                   si.reported_by,
                   ur.name AS reportado_por_nombre,
                   si.title,
                   si.description,
                   si.category,
                   si.priority,
                   si.status,
                   si.resolution_notes,
                   si.created_at,
                   si.updated_at
            FROM security_incident si
            INNER JOIN user ua ON ua.id_user = si.id_user_affected
            INNER JOIN user ur ON ur.id_user = si.reported_by
            WHERE 1=1
            """);

        List<Object> parametros = new ArrayList<>();

        if (idUser != null && idUser > 0) {
            sql.append(" AND si.id_user_affected = ?");
            parametros.add(idUser);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND si.status = ?");
            parametros.add(status.trim().toUpperCase());
        }

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append(" AND si.category = ?");
            parametros.add(category.trim().toUpperCase());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append(" AND si.priority = ?");
            parametros.add(priority.trim().toUpperCase());
        }

        sql.append(" ORDER BY si.id_incident DESC LIMIT 100");

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
                        JsonObject inc = new JsonObject();
                        inc.addProperty("idIncident", rs.getInt("id_incident"));
                        inc.addProperty("idUserAffected", rs.getInt("id_user_affected"));
                        inc.addProperty("usuarioAfectadoNombre", rs.getString("usuario_afectado_nombre"));
                        inc.addProperty("usuarioAfectadoEmail", rs.getString("usuario_afectado_email"));
                        inc.addProperty("reportedBy", rs.getInt("reported_by"));
                        inc.addProperty("reportadoPorNombre", rs.getString("reportado_por_nombre"));
                        inc.addProperty("title", rs.getString("title"));
                        inc.addProperty("description", rs.getString("description"));
                        inc.addProperty("category", rs.getString("category"));
                        inc.addProperty("priority", rs.getString("priority"));
                        inc.addProperty("status", rs.getString("status"));
                        inc.addProperty("resolutionNotes", rs.getString("resolution_notes"));
                        inc.addProperty("createdAt", rs.getString("created_at"));
                        inc.addProperty("updatedAt", rs.getString("updated_at"));
                        lista.add(inc);
                    }
                }
            }
        }

        return lista;
    }

    // Consulta el detalle de un incidente y su historial de cambios.
    public JsonObject obtenerIncidentePorId(int idIncident) throws SQLException {
        String sql = """
            SELECT si.id_incident,
                   si.id_user_affected,
                   ua.name AS usuario_afectado_nombre,
                   ua.email AS usuario_afectado_email,
                   si.reported_by,
                   ur.name AS reportado_por_nombre,
                   si.title,
                   si.description,
                   si.category,
                   si.priority,
                   si.status,
                   si.resolution_notes,
                   si.created_at,
                   si.updated_at
            FROM security_incident si
            INNER JOIN user ua ON ua.id_user = si.id_user_affected
            INNER JOIN user ur ON ur.id_user = si.reported_by
            WHERE si.id_incident = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idIncident);

                try (ResultSet rs = sentencia.executeQuery()) {
                    if (rs.next()) {
                        JsonObject inc = new JsonObject();
                        inc.addProperty("idIncident", rs.getInt("id_incident"));
                        inc.addProperty("idUserAffected", rs.getInt("id_user_affected"));
                        inc.addProperty("usuarioAfectadoNombre", rs.getString("usuario_afectado_nombre"));
                        inc.addProperty("usuarioAfectadoEmail", rs.getString("usuario_afectado_email"));
                        inc.addProperty("reportedBy", rs.getInt("reported_by"));
                        inc.addProperty("reportadoPorNombre", rs.getString("reportado_por_nombre"));
                        inc.addProperty("title", rs.getString("title"));
                        inc.addProperty("description", rs.getString("description"));
                        inc.addProperty("category", rs.getString("category"));
                        inc.addProperty("priority", rs.getString("priority"));
                        inc.addProperty("status", rs.getString("status"));
                        inc.addProperty("resolutionNotes", rs.getString("resolution_notes"));
                        inc.addProperty("createdAt", rs.getString("created_at"));
                        inc.addProperty("updatedAt", rs.getString("updated_at"));

                        inc.add("historial", obtenerHistorialConConexion(conexion, idIncident));
                        return inc;
                    }
                }
            }
        }

        return null;
    }

    // Actualiza el estado de un incidente y guarda el seguimiento en el historial.
    public boolean actualizarEstado(
            int idIncident,
            int actorId,
            String nuevoEstado,
            String comentario,
            String resolutionNotes
    ) throws SQLException {
        String sqlPrevio = "SELECT status FROM security_incident WHERE id_incident = ?";

        String sqlUpdate = """
            UPDATE security_incident
            SET status = ?,
                resolution_notes = COALESCE(?, resolution_notes),
                updated_at = CURRENT_TIMESTAMP
            WHERE id_incident = ?
            """;

        String sqlHistorial = """
            INSERT INTO incident_history
                (id_incident, actor_id, previous_status, new_status, comment)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            conexion.setAutoCommit(false);

            try {
                String estadoAnterior = null;
                try (PreparedStatement psPrev = conexion.prepareStatement(sqlPrevio)) {
                    psPrev.setInt(1, idIncident);
                    try (ResultSet rs = psPrev.executeQuery()) {
                        if (rs.next()) {
                            estadoAnterior = rs.getString("status");
                        } else {
                            conexion.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement psUp = conexion.prepareStatement(sqlUpdate)) {
                    psUp.setString(1, nuevoEstado);
                    psUp.setString(2, resolutionNotes);
                    psUp.setInt(3, idIncident);
                    psUp.executeUpdate();
                }

                try (PreparedStatement psHist = conexion.prepareStatement(sqlHistorial)) {
                    psHist.setInt(1, idIncident);
                    psHist.setInt(2, actorId);
                    psHist.setString(3, estadoAnterior);
                    psHist.setString(4, nuevoEstado);
                    psHist.setString(5, comentario != null ? comentario : "Cambio de estado a " + nuevoEstado);
                    psHist.executeUpdate();
                }

                conexion.commit();
                return true;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    // Consulta el historial de cambios de un incidente.
    public JsonArray obtenerHistorial(int idIncident) throws SQLException {
        try (Connection conexion = ConexionDB.obtenerConexion()) {
            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }
            return obtenerHistorialConConexion(conexion, idIncident);
        }
    }

    private JsonArray obtenerHistorialConConexion(Connection conexion, int idIncident) throws SQLException {
        String sql = """
            SELECT ih.id_history,
                   ih.actor_id,
                   u.name AS actor_nombre,
                   ih.previous_status,
                   ih.new_status,
                   ih.comment,
                   ih.created_at
            FROM incident_history ih
            INNER JOIN user u ON u.id_user = ih.actor_id
            WHERE ih.id_incident = ?
            ORDER BY ih.id_history ASC
            """;

        JsonArray historial = new JsonArray();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idIncident);

            try (ResultSet rs = sentencia.executeQuery()) {
                while (rs.next()) {
                    JsonObject h = new JsonObject();
                    h.addProperty("idHistory", rs.getInt("id_history"));
                    h.addProperty("actorId", rs.getInt("actor_id"));
                    h.addProperty("actorNombre", rs.getString("actor_nombre"));
                    h.addProperty("previousStatus", rs.getString("previous_status"));
                    h.addProperty("newStatus", rs.getString("new_status"));
                    h.addProperty("comment", rs.getString("comment"));
                    h.addProperty("createdAt", rs.getString("created_at"));
                    historial.add(h);
                }
            }
        }

        return historial;
    }
}
