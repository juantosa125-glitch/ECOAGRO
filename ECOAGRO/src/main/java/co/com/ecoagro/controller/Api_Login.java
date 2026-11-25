package co.com.ecoagro.controller;

import co.com.ecoagro.dao.LoginDAO;
import co.com.ecoagro.security.PasswordService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** Atiende las dos rutas de inicio de sesión que reciben credenciales en JSON. */
@WebServlet({"/api/Api_Login", "/api/login"})
public class Api_Login extends HttpServlet {
    private final LoginDAO loginDAO = new LoginDAO();
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    /** Valida las credenciales de la petición y comunica el resultado de la autenticación. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        boolean legacy = "/api/login".equals(request.getServletPath());

        try {
            Object cuerpo = gson.fromJson(request.getReader(), Object.class);
            if (!(cuerpo instanceof Map<?, ?> datos) || !(datos.get("identifier") instanceof String identifier)
                    || !(datos.get("password") instanceof String password)) {
                responder(response, legacy, 400, legacy ? "Los campos identifier y password son obligatorios."
                        : "Se requieren identifier y password como texto.");
                return;
            }
            // La ruta histórica admite identificadores con un formato más amplio.
            if (legacy && (identifier.isBlank() || password.isBlank())) {
                responder(response, true, 400, "El identificador y la contraseña no pueden estar vacíos.");
                return;
            }
            if (!legacy && !identifier.matches("^[a-zA-Z0-9@._-]+$")) {
                responder(response, false, 400, "Formato inválido. No uses espacios ni caracteres extraños.");
                return;
            }
            if (!PasswordService.longitudValida(password)) {
                responder(response, legacy, 400, "La contraseña debe tener entre 1 y 72 bytes en UTF-8.");
                return;
            }

            if (loginDAO.autenticar(identifier, password)) {
                responder(response, legacy, 200, "Inicio de sesión exitoso.");
            } else {
                responder(response, legacy, 401, legacy ? "El identificador o la contraseña son incorrectos."
                        : "El correo, celular o la contraseña son incorrectos.");
            }
        } catch (JsonParseException e) {
            responder(response, legacy, 400, "La solicitud no tiene un formato válido.");
        } catch (SQLException e) {
            responder(response, legacy, 500, "Error interno al consultar la base de datos.");
        }
    }

    /** Escribe el estado HTTP y los campos JSON que espera cada ruta. */
    private void responder(HttpServletResponse response, boolean legacy, int status, String mensaje)
            throws IOException {
        boolean exito = status == HttpServletResponse.SC_OK;
        JsonObject resultado = new JsonObject();
        if (legacy) {
            resultado.addProperty("success", exito);
            resultado.addProperty("message", mensaje);
        } else {
            resultado.addProperty(exito ? "mensaje" : "error", mensaje);
        }
        response.setStatus(status);
        gson.toJson(resultado, response.getWriter());
    }
}
