package co.com.ecoagro.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import co.com.ecoagro.dao.IncidentDAO;
import co.com.ecoagro.dao.SecurityLogDAO;
import co.com.ecoagro.dao.SecurityUserDAO;
import co.com.ecoagro.security.StaffAuthorization;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// API para la creación, consulta y seguimiento de incidentes de seguridad.
@WebServlet("/api/Api_Security_Incidents")
public class Api_Security_Incidents extends HttpServlet {

    // Lee el cuerpo JSON de la solicitud.
    private JsonObject leerCuerpoJson(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                sb.append(linea);
            }
        }

        String cuerpo = sb.toString().trim();
        if (cuerpo.isEmpty()) {
            return null;
        }

        try {
            return JsonParser.parseString(cuerpo).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
    }

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

    // Consulta incidentes o el detalle de un incidente específico con su historial.
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

            IncidentDAO incidentDAO = new IncidentDAO();

            String idIncidentParam = request.getParameter("idIncident");
            if (idIncidentParam != null && !idIncidentParam.trim().isEmpty()) {
                int idIncident = Integer.parseInt(idIncidentParam.trim());
                JsonObject incidente = incidentDAO.obtenerIncidentePorId(idIncident);

                if (incidente == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Incidente no encontrado.");
                    response.getWriter().print(error.toString());
                    return;
                }

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(incidente.toString());
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

            String status = request.getParameter("status");
            String category = request.getParameter("category");
            String priority = request.getParameter("priority");

            JsonArray incidentes = incidentDAO.listarIncidentes(idUser, status, category, priority);

            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("total", incidentes.size());
            respuesta.add("incidentes", incidentes);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Incidents doGet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al consultar incidentes.");
            response.getWriter().print(error.toString());
        }
    }

    // Registra un nuevo incidente de seguridad.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer actorId = validarSesionAdmin(request, response);
            if (actorId == null) {
                return;
            }

            JsonObject cuerpo = leerCuerpoJson(request);
            if (cuerpo == null
                    || !cuerpo.has("idUserAffected")
                    || !cuerpo.has("title")
                    || !cuerpo.has("description")
                    || !cuerpo.has("category")) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Faltan campos obligatorios: idUserAffected, title, description, category.");
                response.getWriter().print(err.toString());
                return;
            }

            int idUserAffected = cuerpo.get("idUserAffected").getAsInt();
            String title = cuerpo.get("title").getAsString().trim();
            String description = cuerpo.get("description").getAsString().trim();
            String category = cuerpo.get("category").getAsString().trim().toUpperCase();
            String priority = cuerpo.has("priority") ? cuerpo.get("priority").getAsString().trim().toUpperCase() : "MEDIA";

            if (title.isEmpty() || description.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "El título y la descripción no pueden estar vacíos.");
                response.getWriter().print(err.toString());
                return;
            }

            SecurityUserDAO userDAO = new SecurityUserDAO();
            if (!userDAO.existeUsuario(idUserAffected)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject err = new JsonObject();
                err.addProperty("error", "El usuario afectado no existe.");
                response.getWriter().print(err.toString());
                return;
            }

            IncidentDAO incidentDAO = new IncidentDAO();
            int idGenerado = incidentDAO.crearIncidente(
                    idUserAffected,
                    actorId,
                    title,
                    description,
                    category,
                    priority
            );

            new SecurityLogDAO().registrarEvento(
                    idUserAffected,
                    actorId,
                    "INCIDENT_CREATED",
                    "Incidente #" + idGenerado + " creado: " + title,
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_CREATED);
            JsonObject res = new JsonObject();
            res.addProperty("mensaje", "Incidente de seguridad registrado correctamente.");
            res.addProperty("idIncident", idGenerado);
            response.getWriter().print(res.toString());

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Incidents doPost", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al crear el incidente.");
            response.getWriter().print(error.toString());
        }
    }

    // Actualiza el estado y seguimiento de un incidente.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer actorId = validarSesionAdmin(request, response);
            if (actorId == null) {
                return;
            }

            JsonObject cuerpo = leerCuerpoJson(request);
            if (cuerpo == null || !cuerpo.has("idIncident") || !cuerpo.has("status")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Se requiere idIncident y status (OPEN, IN_REVIEW, RESOLVED, CLOSED).");
                response.getWriter().print(err.toString());
                return;
            }

            int idIncident = cuerpo.get("idIncident").getAsInt();
            String nuevoEstado = cuerpo.get("status").getAsString().trim().toUpperCase();
            String comentario = cuerpo.has("comment") ? cuerpo.get("comment").getAsString().trim() : null;
            String resolutionNotes = cuerpo.has("resolutionNotes") ? cuerpo.get("resolutionNotes").getAsString().trim() : null;

            if (!nuevoEstado.equals("OPEN") && !nuevoEstado.equals("IN_REVIEW")
                    && !nuevoEstado.equals("RESOLVED") && !nuevoEstado.equals("CLOSED")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Estado no válido. Opciones: OPEN, IN_REVIEW, RESOLVED, CLOSED.");
                response.getWriter().print(err.toString());
                return;
            }

            IncidentDAO incidentDAO = new IncidentDAO();
            boolean actualizado = incidentDAO.actualizarEstado(
                    idIncident,
                    actorId,
                    nuevoEstado,
                    comentario,
                    resolutionNotes
            );

            if (!actualizado) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject err = new JsonObject();
                err.addProperty("error", "El incidente no existe.");
                response.getWriter().print(err.toString());
                return;
            }

            new SecurityLogDAO().registrarEvento(
                    null,
                    actorId,
                    "INCIDENT_UPDATED",
                    "Incidente #" + idIncident + " actualizado a " + nuevoEstado,
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_OK);
            JsonObject res = new JsonObject();
            res.addProperty("mensaje", "Estado del incidente actualizado correctamente.");
            response.getWriter().print(res.toString());

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Incidents doPut", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al actualizar el incidente.");
            response.getWriter().print(error.toString());
        }
    }
}
