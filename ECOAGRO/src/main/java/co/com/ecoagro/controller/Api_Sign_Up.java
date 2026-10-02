package co.com.ecoagro.controller;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/api/Api_Sign_Up")
public class Api_Sign_Up extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. PREPARAR LA RESPUESTA DE LA API
        // Indicamos que la API responderá exclusivamente en formato JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        var out = response.getWriter();

        try {
            // 2. LEER Y TRADUCIR EL JSON RECIBIDO DEL FRONTEND O POSTMAN
            BufferedReader reader = request.getReader();
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            // Validar que el cuerpo de la petición no esté vacío
            if (jsonObject == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Error 400
                out.print("{\"error\": \"No enviaste ningún dato en formato JSON\"}");
                return;
            }

            // 3. EXTRAER LAS VARIABLES DEL JSON
            String nombre = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "";
            String correo = jsonObject.has("email") ? jsonObject.get("email").getAsString() : "";
            String celular = jsonObject.has("number") ? jsonObject.get("number").getAsString() : "";
            String password = jsonObject.has("password") ? jsonObject.get("password").getAsString() : "";
            String confirmPassword = jsonObject.has("confirm_password") ? jsonObject.get("confirm_password").getAsString() : "";

            // 4. VALIDACIONES DE REGLAS DE NEGOCIO
            if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Código 400: Datos incorrectos del cliente
                out.print("{\"error\": \"El nombre solo debe contener letras.\"}");
                return;
            }

            if (!celular.matches("^[0-9]+$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
                out.print("{\"error\": \"El número de teléfono no es válido.\"}");
                return;
            }

            if (password.isEmpty() || !password.equals(confirmPassword)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
                out.print("{\"error\": \"Las contraseñas no coinciden.\"}");
                return;
            }

            // 5. INSERCIÓN EN LA BASE DE DATOS
            // Se usa try-with-resources para que la conexión se cierre automáticamente
            try (Connection conn = ConexionDB.obtenerConexion()) {
                if (conn != null) {
                    // Preparamos la consulta SQL
                    String sql = "INSERT INTO user (name, email, number, password) VALUES (?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    
                    ps.setString(1, nombre);
                    ps.setString(2, correo);
                    ps.setString(3, celular);
                    ps.setString(4, password); // Nota: En un entorno real, aquí se encriptaría la contraseña
                    
                    // Ejecutamos la inserción
                    int filasInsertadas = ps.executeUpdate();
                    
                    if (filasInsertadas > 0) {
                        // Código 201: El recurso fue creado correctamente
                        response.setStatus(HttpServletResponse.SC_CREATED); 
                        out.print("{\"mensaje\": \"Registro satisfactorio en EcoAgro\"}");
                    } else {
                        // Código 400 si falló la inserción lógica
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.print("{\"error\": \"No se pudo registrar el usuario en la base de datos.\"}");
                    }
                } else {
                    // Código 500: Error del servidor (Problemas con MySQL)
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
                    out.print("{\"error\": \"Error interno: No hay conexión a la base de datos.\"}");
                }
            }

        } catch (Exception e) {
            // Código 500: Error crítico en el servidor
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            out.print("{\"error\": \"Error en el servidor: " + e.getMessage() + "\"}");
        }
        
        // Limpiamos y enviamos la respuesta
        out.flush();
    }
}