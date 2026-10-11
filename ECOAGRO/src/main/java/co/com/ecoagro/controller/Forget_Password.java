package co.com.ecoagro.controllers;

import co.com.ecoagro.config.ConexionDB;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/Forget-Password")
public class Forget_Password extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // 1. Leer el JSON crudo (raw) desde el body de la petición
            StringBuilder jsonRecibido = new StringBuilder();
            String linea;
            try (BufferedReader reader = request.getReader()) {
                while ((linea = reader.readLine()) != null) {
                    jsonRecibido.append(linea);
                }
            }

            // 2. Convertir el texto a un JsonObject usando Gson
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonRecibido.toString(), JsonObject.class);

            // 3. Extraer los valores (¡Estas llaves deben coincidir exactamente con las de Postman!)
            String email = jsonObject.get("email").getAsString();
            String nuevaPassword = jsonObject.get("nuevaPassword").getAsString();

            // 4. Lógica de Base de Datos
            String sql = "UPDATE user SET password = ? WHERE email = ?";

            try (Connection conn = ConexionDB.obtenerConexion(); 
                 PreparedStatement pstmt = (conn != null) ? conn.prepareStatement(sql) : null) {

                if (conn != null) {
                    pstmt.setString(1, nuevaPassword); // Aquí en un futuro se integraría BCrypt
                    pstmt.setString(2, email);

                    int filasAfectadas = pstmt.executeUpdate();

                    if (filasAfectadas > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("{\"status\": \"success\", \"mensaje\": \"Contraseña actualizada correctamente.\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"status\": \"error\", \"mensaje\": \"No existe un usuario registrado con este correo.\"}");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"status\": \"error\", \"mensaje\": \"Error de conexión con la base de datos.\"}");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\": \"error\", \"mensaje\": \"Error interno procesando la base de datos.\"}");
            }

        } catch (JsonSyntaxException | NullPointerException e) {
            // Captura el error si el JSON está mal formado o faltan las llaves "email" o "nuevaPassword"
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\": \"error\", \"mensaje\": \"El formato del JSON es inválido o faltan datos requeridos.\"}");
        }
    }
}