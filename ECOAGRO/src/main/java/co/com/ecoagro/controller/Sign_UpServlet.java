package co.com.ecoagro.controller;

import co.com.ecoagro.config.ConexionDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/sign_up")
public class Sign_UpServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Recibir los datos del formulario
        String nombre = request.getParameter("name");
        String correo = request.getParameter("email");
        String celular = request.getParameter("number");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password"); 

        // 2. Validar que el nombre solo tenga letras (sin números ni caracteres especiales)
        if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
            request.setAttribute("errorAuth", "El nombre solo debe contener letras, sin números ni caracteres especiales.");
            request.getRequestDispatcher("sign_up.jsp").forward(request, response);
            return;
        }

        // 3. Validar que el teléfono solo tenga números positivos (sin letras ni signos -)
        if (!celular.matches("^[0-9]+$")) {
            request.setAttribute("errorAuth", "El número de teléfono no es válido. Solo se aceptan números positivos.");
            request.getRequestDispatcher("sign_up.jsp").forward(request, response);
            return;
        }

        // 4. Validar que las contraseñas coincidan
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            request.setAttribute("errorAuth", "Las contraseñas no coinciden. Inténtalo de nuevo.");
            request.getRequestDispatcher("sign_up.jsp").forward(request, response);
            return;
        }

        // 5. Conectar a la base de datos e insertar
        try (Connection conn = ConexionDB.obtenerConexion()) {
            
            if (conn != null) {
                String sql = "INSERT INTO user (name, email, number, password) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                
                ps.setString(1, nombre);
                ps.setString(2, correo);
                ps.setString(3, celular);
                ps.setString(4, password);
                
                int filasInsertadas = ps.executeUpdate();
                
                if (filasInsertadas > 0) {
                    // Si el registro es exitoso, envía a la pantalla de Sign In (Login)
                    response.sendRedirect("login.jsp"); 
                } else {
                    request.setAttribute("errorAuth", "No se pudo registrar el usuario.");
                    request.getRequestDispatcher("sign_up.jsp").forward(request, response);
                }
            } else {
                response.setContentType("text/html;charset=UTF-8");
                try (var out = response.getWriter()) {
                    out.println("<h3>Error de conexión:</h3>");
                    out.println("<p>La clase ConexionDB devolvió una conexión nula.</p>");
                }
            }
            
        } catch (Exception e) {
            response.setContentType("text/html;charset=UTF-8");
            try (var out = response.getWriter()) {
                out.println("<h3>Ocurrió un error crítico en el Servlet:</h3>");
                out.println("<p>" + e.getMessage() + "</p>");
                e.printStackTrace(out); 
            }
        }
    }
}