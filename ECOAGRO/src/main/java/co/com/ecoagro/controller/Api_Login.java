package co.com.ecoagro.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import co.com.ecoagro.config.ConexionDB;
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

        // 1. Configurar la respuesta como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        var out = response.getWriter();

        try {
            // 2. Leer el cuerpo de la petición (JSON) enviado por JavaScript
            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            if (jsonObject == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"No se enviaron datos.\"}");
                return;
            }

            // 3. Extraer las credenciales
            String identifier = jsonObject.has("identifier") ? jsonObject.get("identifier").getAsString() : "";
            String password = jsonObject.has("password") ? jsonObject.get("password").getAsString() : "";

            // 4. Validación de formato estricta
            if (!identifier.matches("^[a-zA-Z0-9@._-]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                out.print("{\"error\": \"Formato inválido. No uses espacios ni caracteres extraños.\"}");
                return;
            }

            // 5. Conexión y consulta a la base de datos
            try (Connection conn = ConexionDB.obtenerConexion()) {
                if (conn != null) {
                    // Consulta que busca coincidencias en correo o celular
                    String sql = """
                    SELECT id_user, name, email
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

                        // Evitar que la sesión permanezca autenticada indefinidamente.
                        session.setMaxInactiveInterval(30 * 60);

                        response.setStatus(HttpServletResponse.SC_OK);

                        JsonObject respuesta = new JsonObject();
                        respuesta.addProperty(
                                "mensaje",
                                "Inicio de sesión exitoso."
                        );
                        respuesta.addProperty("autenticado", true);
                        respuesta.addProperty("rango", rango);
                        out.print(gson.toJson(respuesta));
                    } else {
                        // Código 401: No autorizado (credenciales incorrectas)
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print("{\"error\": \"El correo, celular o la contraseña son incorrectos.\"}");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                    out.print("{\"error\": \"Error interno: No se pudo conectar a la base de datos.\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace(); // Registrar el error en la consola del servidor.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            JsonObject error = new JsonObject();
            error.addProperty("error", "Ocurrió un error interno en el servidor.");

            out.print(error.toString());
        }

        out.flush();
    }
}
