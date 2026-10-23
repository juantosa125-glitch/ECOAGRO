package co.com.ecoagro.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import co.com.ecoagro.config.ConexionDB;
import co.com.ecoagro.dao.SecurityLogDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/Api_Login")
public class Api_Login extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        var out = response.getWriter();
        SecurityLogDAO logDAO = new SecurityLogDAO();

        try {
            request.setCharacterEncoding("UTF-8");
            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            if (jsonObject == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"No se enviaron datos.\"}");
                return;
            }

            String identifier = jsonObject.has("identifier") ? jsonObject.get("identifier").getAsString() : "";
            String password = jsonObject.has("password") ? jsonObject.get("password").getAsString() : "";

            if (!identifier.matches("^[a-zA-Z0-9@._-]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Formato inválido. No uses espacios ni caracteres extraños.\"}");
                return;
            }

            try (Connection conn = ConexionDB.obtenerConexion()) {
                if (conn != null) {
                    String sql = """
                    SELECT id_user, name, email, account_status, status_reason
                    FROM `user`
                    WHERE (email = ? OR number = ?)
                    AND password = ?
                    """;
                    PreparedStatement ps = conn.prepareStatement(sql);

                    ps.setString(1, identifier);
                    ps.setString(2, identifier);
                    ps.setString(3, password);

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        int idUsuario = rs.getInt("id_user");
                        String nombre = rs.getString("name");
                        String email = rs.getString("email");
                        String estado = rs.getString("account_status");
                        String motivo = rs.getString("status_reason");

                        // Verificar si la cuenta está suspendida.
                        if ("SUSPENDED".equalsIgnoreCase(estado)) {
                            logDAO.registrarEvento(
                                    idUsuario,
                                    idUsuario,
                                    "LOGIN_FAILED",
                                    "Intento de inicio de sesión con cuenta suspendida.",
                                    request.getRemoteAddr()
                            );

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            JsonObject err = new JsonObject();
                            err.addProperty("error", "Tu cuenta se encuentra suspendida.");
                            err.addProperty("motivo", motivo != null ? motivo : "No especificado");
                            out.print(err.toString());
                            return;
                        }

                        // Crear la sesión autenticada en el servidor.
                        var session = request.getSession(true);
                        session.setAttribute("idUsuario", idUsuario);
                        session.setAttribute("nombreUsuario", nombre);
                        session.setAttribute("emailUsuario", email);

                        String rango = "USUARIO";

                        String sqlRango = """
                            SELECT sr.role_name
                            FROM staff_assignment sa
                            INNER JOIN staff_role sr
                            ON sr.id_role = sa.id_role
                            WHERE sa.id_user = ?
                        """;

                        try (PreparedStatement psRango = conn.prepareStatement(sqlRango)) {
                            psRango.setInt(1, idUsuario);

                            try (ResultSet rsRango = psRango.executeQuery()) {
                                if (rsRango.next()) {
                                    rango = rsRango.getString("role_name");
                                }
                            }
                        }

                        session.setAttribute("rangoUsuario", rango);
                        session.setMaxInactiveInterval(30 * 60);

                        logDAO.registrarEvento(
                                idUsuario,
                                idUsuario,
                                "LOGIN_SUCCESS",
                                "Inicio de sesión exitoso.",
                                request.getRemoteAddr()
                        );

                        response.setStatus(HttpServletResponse.SC_OK);

                        JsonObject respuesta = new JsonObject();
                        respuesta.addProperty("mensaje", "Inicio de sesión exitoso.");
                        respuesta.addProperty("autenticado", true);
                        respuesta.addProperty("rango", rango);
                        out.print(gson.toJson(respuesta));
                    } else {
                        logDAO.registrarEvento(
                                null,
                                null,
                                "LOGIN_FAILED",
                                "Credenciales incorrectas para: " + identifier,
                                request.getRemoteAddr()
                        );

                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print("{\"error\": \"El correo, celular o la contraseña son incorrectos.\"}");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\": \"Error interno: No se pudo conectar a la base de datos.\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            JsonObject error = new JsonObject();
            error.addProperty("error", "Ocurrió un error interno en el servidor.");
            out.print(error.toString());
        }

        out.flush();
    }

    // Cierre de sesión seguro en el servidor.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        var session = request.getSession(false);
        if (session != null) {
            Integer idUsuario = (Integer) session.getAttribute("idUsuario");
            if (idUsuario != null) {
                new SecurityLogDAO().registrarEvento(
                        idUsuario,
                        idUsuario,
                        "LOGOUT",
                        "Cierre de sesión del usuario.",
                        request.getRemoteAddr()
                );
            }
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{\"mensaje\": \"Sesión cerrada correctamente.\"}");
    }
}
