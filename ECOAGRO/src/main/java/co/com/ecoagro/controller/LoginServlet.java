package co.com.ecoagro.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Recibe los parámetros del formulario
        String correo = request.getParameter("email");
        String password = request.getParameter("password");

        // Condicional de validación
        if ("admin@gmail.com".equals(correo) && "1234".equals(password)) {
            
            // Si las credenciales son correctas, redirige a la página principal
            response.sendRedirect("users/user_comprador/dashboard_comprador.html");

        } else {
            
            // 1. Guardamos el mensaje de error en un atributo de la petición
            request.setAttribute("errorAuth", "El correo o la contraseña son incorrectos.");
            
            // 2. Reenviamos la petición de vuelta a la vista (debe ser .jsp)
            request.getRequestDispatcher("login.jsp").forward(request, response);

        }
    }
}