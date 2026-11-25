package co.com.ecoagro.controller;

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

/** Atiende las solicitudes de registro de usuarios en formato JSON. */
@WebServlet("/api/Api_Sign_Up")
public class Api_Sign_Up extends HttpServlet {
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    /** Comprueba los campos del registro y devuelve el resultado del alta. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        JsonObject resultado = new JsonObject();
        int status = HttpServletResponse.SC_BAD_REQUEST;

        try {
            Object cuerpo = gson.fromJson(request.getReader(), Object.class);
            if (!(cuerpo instanceof Map<?, ?> datos)) {
                resultado.addProperty("error", "Envía los datos de registro en un objeto JSON.");
            } else if (datos.get("name") instanceof String nombre && datos.get("email") instanceof String correo
                    && datos.get("number") instanceof String celular && datos.get("password") instanceof String password
                    && datos.get("confirm_password") instanceof String confirmacion) {
                // La API y el formulario comparten la validación y el alta de usuarios.
                String error = Sign_UpServlet.validarRegistro(nombre, correo, celular, password, confirmacion);
                if (error != null) {
                    resultado.addProperty("error", error);
                } else if (Sign_UpServlet.registrar(nombre, correo, celular, password)) {
                    status = HttpServletResponse.SC_CREATED;
                    resultado.addProperty("mensaje", "Registro satisfactorio en EcoAgro");
                } else {
                    resultado.addProperty("error", "No se pudo registrar el usuario en la base de datos.");
                }
            } else {
                resultado.addProperty("error", "Todos los campos de registro son obligatorios y deben ser texto.");
            }
        } catch (JsonParseException e) {
            resultado.addProperty("error", "La solicitud no tiene un formato válido.");
        } catch (SQLException e) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            resultado.addProperty("error", "No se pudo registrar el usuario en la base de datos.");
        }

        response.setStatus(status);
        gson.toJson(resultado, response.getWriter());
    }
}
