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
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Recibimos el parámetro unificado (puede ser correo o celular)
        String identifier = request.getParameter("identifier");
        String password = request.getParameter("password");

        // Validación de backend: Permite letras, números, arroba, puntos y guiones bajos. Bloquea espacios y signos negativos.
        if (!identifier.matches("^[a-zA-Z0-9@._-]+$")) {
            request.setAttribute("errorAuth", "Formato inválido. No uses espacios ni caracteres extraños.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Conectar a la base de datos y validar
        try (Connection conn = ConexionDB.obtenerConexion()) {
            
            if (conn != null) {
                // Buscamos si el dato ingresado coincide con un correo OR con un número
                String sql = "SELECT * FROM user WHERE (email = ? OR number = ?) AND password = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                
                ps.setString(1, identifier); // Compara con la columna email
                ps.setString(2, identifier); // Compara con la columna number
                ps.setString(3, password);   // Compara la contraseña
                
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    // Si encuentra un resultado, los datos son correctos
                    response.sendRedirect("users/user_comprador/dashboard_comprador.html");
                } else {
                    // Si no encuentra nada, las credenciales no existen o la clave está mal
                    request.setAttribute("errorAuth", "El correo, celular o la contraseña son incorrectos.");
                    request.getRequestDispatcher("login.jsp").forward(request, response);
                }
            } else {
                request.setAttribute("errorAuth", "Error interno: No se pudo conectar a la base de datos.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
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