package co.com.ecoagro.controller;

import co.com.ecoagro.dao.LoginDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Gestiona el inicio de sesión mediante parámetros de formulario. */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final LoginDAO loginDAO = new LoginDAO();

    /** Abre el panel del comprador si las credenciales son válidas o devuelve el error a la vista. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String identifier = request.getParameter("identifier");
        String password = request.getParameter("password");

        if (identifier == null || !identifier.matches("^[a-zA-Z0-9@._-]+$")) {
            request.setAttribute("errorAuth", "Formato inválido. No uses espacios ni caracteres extraños.");
        } else {
            try {
                if (loginDAO.autenticar(identifier, password)) {
                    response.sendRedirect(request.getContextPath() + "/users/user_comprador/dashboard_comprador.html");
                    return;
                }
                request.setAttribute("errorAuth", "El correo, celular o la contraseña son incorrectos.");
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                request.setAttribute("errorAuth", "Error interno: No se pudo consultar la base de datos.");
            }
        }
        request.getRequestDispatcher("/Login-SignUp/login.jsp").forward(request, response);
    }
}
