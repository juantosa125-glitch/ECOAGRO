package co.com.ecoagro.dao;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// DAO para gestionar el personal administrativo.
public class StaffDAO {

    // Consultamos la lista del personal administrativo.
    public JsonArray listarPersonalAdministrativo() throws SQLException {

        String sql = """
            SELECT sa.id_assignment,
                   u.id_user,
                   u.name,
                   u.email,
                   u.number,
                   sr.role_name,
                   sa.assigned_at
            FROM staff_assignment sa
            INNER JOIN user u        ON u.id_user   = sa.id_user
            INNER JOIN staff_role sr ON sr.id_role  = sa.id_role
            ORDER BY sa.assigned_at DESC
            """;

        JsonArray lista = new JsonArray();

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet resultado = sentencia.executeQuery()) {

                while (resultado.next()) {
                    JsonObject fila = new JsonObject();
                    fila.addProperty("idAssignment", resultado.getInt("id_assignment"));
                    fila.addProperty("idUser",       resultado.getInt("id_user"));
                    fila.addProperty("nombre",       resultado.getString("name"));
                    fila.addProperty("email",        resultado.getString("email"));
                    fila.addProperty("telefono",     resultado.getString("number"));
                    fila.addProperty("rango",        resultado.getString("role_name"));
                    fila.addProperty("asignadoEl",   resultado.getString("assigned_at"));
                    lista.add(fila);
                }
            }
        }

        return lista;
    }

    // Verificamos si el usuario existe.
    public boolean existeUsuario(int idUser) throws SQLException {

        String sql = "SELECT 1 FROM user WHERE id_user = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Verificamos si el rol existe.
    public boolean existeRol(int idRole) throws SQLException {

        String sql = "SELECT 1 FROM staff_role WHERE id_role = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idRole);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Verificamos si el usuario ya tiene un rango asignado.
    public boolean existeAsignacion(int idUser) throws SQLException {

        String sql = "SELECT 1 FROM staff_assignment WHERE id_user = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Verificamos si la asignación existe por su id.
    public boolean existeAsignacionPorId(int idAssignment) throws SQLException {

        String sql = "SELECT 1 FROM staff_assignment WHERE id_assignment = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idAssignment);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next();
                }
            }
        }
    }

    // Contamos cuántos superadministradores existen en el sistema.
    public int contarSuperadministradores() throws SQLException {

        String sql = """
            SELECT COUNT(*) AS total
            FROM staff_assignment sa
            INNER JOIN staff_role sr ON sr.id_role = sa.id_role
            WHERE sr.role_name = 'SUPERADMINISTRADOR'
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet resultado = sentencia.executeQuery()) {

                if (resultado.next()) {
                    return resultado.getInt("total");
                }
            }
        }

        return 0;
    }

    // Obtenemos el nombre del rol asignado.
    public String obtenerNombreRolDeAsignacion(int idAssignment) throws SQLException {

        String sql = """
            SELECT sr.role_name
            FROM staff_assignment sa
            INNER JOIN staff_role sr ON sr.id_role = sa.id_role
            WHERE sa.id_assignment = ?
            """;

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idAssignment);

                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (resultado.next()) {
                        return resultado.getString("role_name");
                    }
                }
            }
        }

        return null;
    }

    // Guardamos la asignación del rango.
    public void asignarRango(int idUser, int idRole) throws SQLException {

        String sql = "INSERT INTO staff_assignment (id_user, id_role) VALUES (?, ?)";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUser);
                sentencia.setInt(2, idRole);
                sentencia.executeUpdate();
            }
        }
    }

    // Actualizamos el rango de la asignación.
    public void cambiarRango(int idAssignment, int idRole) throws SQLException {

        String sql = "UPDATE staff_assignment SET id_role = ? WHERE id_assignment = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idRole);
                sentencia.setInt(2, idAssignment);
                sentencia.executeUpdate();
            }
        }
    }

    // Eliminamos la asignación del usuario.
    public void retirarAsignacion(int idAssignment) throws SQLException {

        String sql = "DELETE FROM staff_assignment WHERE id_assignment = ?";

        try (Connection conexion = ConexionDB.obtenerConexion()) {

            if (conexion == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idAssignment);
                sentencia.executeUpdate();
            }
        }
    }
}
