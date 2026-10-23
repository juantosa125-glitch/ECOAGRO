package co.com.ecoagro.controller;

import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import co.com.ecoagro.dao.SecurityLogDAO;
import co.com.ecoagro.security.StaffAuthorization;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// API para consultar registros de auditoría y métricas de seguridad.
@WebServlet("/api/Api_Security_Logs")
public class Api_Security_Logs extends HttpServlet {

    // Comprueba que exista sesión activa con permisos administrativos.
    private Integer validarSesionAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        HttpSession sesion = request.getSession(false);

        if (sesion == null || sesion.getAttribute("idUsuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Debes iniciar sesión.");
            response.getWriter().print(error.toString());
            return null;
        }

        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        if (!StaffAuthorization.esPersonalAutorizado(idUsuario)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            JsonObject error = new JsonObject();
            error.addProperty("error", "No tienes permisos de seguridad o administración.");
            response.getWriter().print(error.toString());
            return null;
        }

        return idUsuario;
    }

    // Consulta los eventos de auditoría o las métricas resumidas del panel.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer actorId = validarSesionAdmin(request, response);
            if (actorId == null) {
                return;
            }

            SecurityLogDAO logDAO = new SecurityLogDAO();

            String metricasParam = request.getParameter("metricas");
            if ("true".equalsIgnoreCase(metricasParam)) {
                JsonObject metricas = logDAO.obtenerResumenMetricas();
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(metricas.toString());
                return;
            }

            Integer idUser = null;
            String idUserParam = request.getParameter("idUser");
            if (idUserParam != null && !idUserParam.trim().isEmpty()) {
                try {
                    idUser = Integer.parseInt(idUserParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            String eventType = request.getParameter("eventType");

            int limit = 50;
            String limitParam = request.getParameter("limit");
            if (limitParam != null && !limitParam.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            JsonArray eventos = logDAO.listarEventos(idUser, eventType, limit);

            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("total", eventos.size());
            respuesta.add("eventos", eventos);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Logs doGet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al consultar los registros de seguridad.");
            response.getWriter().print(error.toString());
        }
    }
}
