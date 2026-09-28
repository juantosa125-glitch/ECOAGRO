package co.com.ecoagro.controller;

import co.com.ecoagro.dao.LoginDAO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/login")
public class LoginApiServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final LoginDAO loginDAO = new LoginDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            StringBuilder jsonBody = new StringBuilder();

            try (BufferedReader reader = request.getReader()) {
                String line;

                while ((line = reader.readLine()) != null) {
                    jsonBody.append(line);
                }
            }

            JsonObject json = gson.fromJson(jsonBody.toString(), JsonObject.class);

            if (json == null
                    || !json.has("identifier")
                    || !json.has("password")) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                enviarRespuesta(
                        response,
                        false,
                        "Los campos identifier y password son obligatorios."
                );

                return;
            }

            String identifier = json.get("identifier").getAsString();
            String password = json.get("password").getAsString();

            if (identifier.isBlank() || password.isBlank()) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                enviarRespuesta(
                        response,
                        false,
                        "El identificador y la contraseña no pueden estar vacíos."
                );

                return;
            }

            boolean autenticado = loginDAO.autenticar(identifier, password);

            if (autenticado) {

                response.setStatus(HttpServletResponse.SC_OK);

                enviarRespuesta(
                        response,
                        true,
                        "Inicio de sesión exitoso."
                );

            } else {

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                enviarRespuesta(
                        response,
                        false,
                        "El identificador o la contraseña son incorrectos."
                );
            }

        } catch (SQLException e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            enviarRespuesta(
                    response,
                    false,
                    "Error interno al consultar la base de datos."
            );

            e.printStackTrace();

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            enviarRespuesta(
                    response,
                    false,
                    "La solicitud no tiene un formato válido."
            );

            e.printStackTrace();
        }
    }

    private void enviarRespuesta(
            HttpServletResponse response,
            boolean success,
            String message
    ) throws IOException {

        JsonObject jsonResponse = new JsonObject();

        jsonResponse.addProperty("success", success);
        jsonResponse.addProperty("message", message);

        response.getWriter().write(
                gson.toJson(jsonResponse)
        );
    }
}