package co.com.ecoagro.controller;

import co.com.ecoagro.config.ConexionDB;
import co.com.ecoagro.security.PasswordService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Atiende el formulario de registro y comparte la lógica de alta con la API. */
@WebServlet("/sign_up")
public class Sign_UpServlet extends HttpServlet {

    /** Devuelve el primer error de validación o null si los datos permiten registrar al usuario. */
    static String validarRegistro(String nombre, String correo, String celular, String password, String confirmPassword) {
        if (nombre == null || nombre.isBlank() || nombre.length() > 100
                || !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
            return "El nombre solo debe contener letras.";
        }
        if (correo == null || correo.length() > 100 || !correo.matches("[^@\\s]+@[^@\\s]+")) {
            return "El correo no es válido.";
        }
        if (celular == null || !celular.matches("[0-9]{1,20}")) {
            return "El número de teléfono no es válido.";
        }
        if (!PasswordService.longitudValida(password)) {
            return "La contraseña debe tener entre 1 y 72 bytes en UTF-8.";
        }
        if (!password.equals(confirmPassword)) {
            return "Las contraseñas no coinciden.";
        }
        return null;
    }

    /** Inserta el usuario con el hash de su contraseña e indica si se creó la cuenta. */
    static boolean registrar(String nombre, String correo, String celular, String password) throws SQLException {
        String hash = PasswordService.generarHash(password);
        String sql = "INSERT INTO user (name, email, number, password) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionDB.obtenerConexion()) {
            if (conn == null) {
                throw new SQLException("No fue posible conectar con la base de datos.");
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, correo);
                ps.setString(3, celular);
                ps.setString(4, hash);
                return ps.executeUpdate() > 0;
            }
        }
    }

    /** Procesa el alta del formulario y dirige al usuario al inicio de sesión si tiene éxito. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String nombre = request.getParameter("name");
        String correo = request.getParameter("email");
        String celular = request.getParameter("number");
        String password = request.getParameter("password");
        String error = validarRegistro(nombre, correo, celular, password, request.getParameter("confirm_password"));
        if (error == null) {
            try {
                if (registrar(nombre, correo, celular, password)) {
                    response.sendRedirect(request.getContextPath() + "/Login-SignUp/login.jsp");
                    return;
                }
                error = "No se pudo registrar el usuario.";
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                error = "No se pudo registrar el usuario.";
            }
        }
        request.setAttribute("errorAuth", error);
        request.getRequestDispatcher("/Login-SignUp/sign_up.jsp").forward(request, response);
    }
}
