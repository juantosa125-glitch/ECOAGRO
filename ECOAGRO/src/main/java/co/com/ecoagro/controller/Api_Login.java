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
import java.sql.ResultSet;

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
                    String sql = "SELECT * FROM user WHERE (email = ? OR number = ?) AND password = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    
                    ps.setString(1, identifier); 
                    ps.setString(2, identifier); 
                    ps.setString(3, password);   
                    
                    ResultSet rs = ps.executeQuery();
                    
                    if (rs.next()) {
                        // Código 200: Credenciales válidas
                        response.setStatus(HttpServletResponse.SC_OK); 
                        out.print("{\"mensaje\": \"Inicio de sesión exitoso.\"}");
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Ocurrió un error crítico: " + e.getMessage() + "\"}");
        }
        
        out.flush();
    }
}