package co.com.ecoagro.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/Api_Sign_Up")
public class Api_Sign_Up extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Le decimos a Postman que le vamos a responder con un JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        var out = response.getWriter();

        try {
            // 2. LEER EL TEXTO JSON QUE ESCRIBISTE EN POSTMAN
            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            if (jsonObject == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"No enviaste ningún JSON\"}");
                return;
            }

            // 3. Extraer las variables del JSON recibido
            String nombre = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "";
            String correo = jsonObject.has("email") ? jsonObject.get("email").getAsString() : "";
            String celular = jsonObject.has("number") ? jsonObject.get("number").getAsString() : "";
            String password = jsonObject.has("password") ? jsonObject.get("password").getAsString() : "";
            String confirmPassword = jsonObject.has("confirm_password") ? jsonObject.get("confirm_password").getAsString() : "";

            // 4. EJECUTAR TUS VALIDACIONES
            if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                out.print("{\"error\": \"El nombre solo debe contener letras.\"}");
                return;
            }

            if (!celular.matches("^[0-9]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                out.print("{\"error\": \"El número de teléfono no es válido.\"}");
                return;
            }

            if (password.isEmpty() || !password.equals(confirmPassword)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                out.print("{\"error\": \"Las contraseñas no coinciden.\"}");
                return;
            }

            // Si pasa todas las validaciones (aquí iría tu código de base de datos de EcoAgro)
            // Por ahora, solo confirmamos que la validación fue un éxito:
            response.setStatus(HttpServletResponse.SC_OK); // 200
            out.print("{\"mensaje\": \"Validación exitosa. Los datos son correctos.\"}");
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            out.print("{\"error\": \"Error en el servidor: " + e.getMessage() + "\"}");
        }
        
        out.flush();
    }
}