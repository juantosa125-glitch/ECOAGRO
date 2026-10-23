package co.com.ecoagro.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import co.com.ecoagro.dao.SecurityLogDAO;
import co.com.ecoagro.dao.SecurityUserDAO;
import co.com.ecoagro.security.StaffAuthorization;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// API para la supervisión de usuarios y gestión de estados de cuenta.
@WebServlet("/api/Api_Security_Users")
public class Api_Security_Users extends HttpServlet {

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

    // Consulta usuarios o el detalle de un usuario específico.
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

            SecurityUserDAO userDAO = new SecurityUserDAO();

            String idUserParam = request.getParameter("idUser");
            if (idUserParam != null && !idUserParam.trim().isEmpty()) {
                int idUser = Integer.parseInt(idUserParam.trim());
                JsonObject detalle = userDAO.obtenerDetalleUsuario(idUser);

                if (detalle == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Usuario no encontrado.");
                    response.getWriter().print(error.toString());
                    return;
                }

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(detalle.toString());
                return;
            }

            String busqueda = request.getParameter("busqueda");
            String tipo = request.getParameter("tipo");
            String estado = request.getParameter("estado");

            int limit = 20;
            int offset = 0;

            String limitParam = request.getParameter("limit");
            String offsetParam = request.getParameter("offset");

            if (limitParam != null && !limitParam.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            if (offsetParam != null && !offsetParam.trim().isEmpty()) {
                try {
                    offset = Integer.parseInt(offsetParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            JsonArray usuarios = userDAO.listarUsuarios(busqueda, tipo, estado, limit, offset);

            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("total", usuarios.size());
            respuesta.add("usuarios", usuarios);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Users doGet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al consultar usuarios.");
            response.getWriter().print(error.toString());
        }
    }

    // Suspende o reactiva la cuenta de un usuario.
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
            if (cuerpo == null || !cuerpo.has("idUser") || !cuerpo.has("action")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Se requiere idUser y action (SUSPEND o REACTIVATE).");
                response.getWriter().print(err.toString());
                return;
            }

            int idUser = cuerpo.get("idUser").getAsInt();
            String accion = cuerpo.get("action").getAsString().trim().toUpperCase();
            String motivo = cuerpo.has("reason") ? cuerpo.get("reason").getAsString().trim() : "";

            // No permitir suspenderse a sí mismo.
            if (idUser == actorId) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                JsonObject err = new JsonObject();
                err.addProperty("error", "No puedes modificar el estado de tu propia cuenta.");
                response.getWriter().print(err.toString());
                return;
            }

            // No permitir modificar cuentas de otros administradores del staff desde este endpoint.
            if (StaffAuthorization.esMiembroStaff(idUser)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JsonObject err = new JsonObject();
                err.addProperty("error", "No es posible suspender la cuenta de un miembro del personal administrativo.");
                response.getWriter().print(err.toString());
                return;
            }

            SecurityUserDAO userDAO = new SecurityUserDAO();
            if (!userDAO.existeUsuario(idUser)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject err = new JsonObject();
                err.addProperty("error", "El usuario no existe.");
                response.getWriter().print(err.toString());
                return;
            }

            String estadoActual = userDAO.obtenerEstadoCuenta(idUser);
            SecurityLogDAO logDAO = new SecurityLogDAO();

            if ("SUSPEND".equals(accion)) {
                if (motivo.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject err = new JsonObject();
                    err.addProperty("error", "El motivo de suspensión es obligatorio.");
                    response.getWriter().print(err.toString());
                    return;
                }

                if ("SUSPENDED".equalsIgnoreCase(estadoActual)) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    JsonObject err = new JsonObject();
                    err.addProperty("error", "La cuenta ya se encuentra suspendida.");
                    response.getWriter().print(err.toString());
                    return;
                }

                userDAO.cambiarEstadoCuenta(idUser, "SUSPENDED", motivo);

                logDAO.registrarEvento(
                        idUser,
                        actorId,
                        "ACCOUNT_SUSPENDED",
                        "Cuenta suspendida. Motivo: " + motivo,
                        request.getRemoteAddr()
                );

                JsonObject res = new JsonObject();
                res.addProperty("mensaje", "La cuenta fue suspendida correctamente.");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(res.toString());

            } else if ("REACTIVATE".equals(accion)) {
                if ("ACTIVE".equalsIgnoreCase(estadoActual)) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    JsonObject err = new JsonObject();
                    err.addProperty("error", "La cuenta ya se encuentra activa.");
                    response.getWriter().print(err.toString());
                    return;
                }

                String motivoReactivacion = motivo.isEmpty() ? "Reactivación por el personal de seguridad." : motivo;
                userDAO.cambiarEstadoCuenta(idUser, "ACTIVE", motivoReactivacion);

                logDAO.registrarEvento(
                        idUser,
                        actorId,
                        "ACCOUNT_REACTIVATED",
                        "Cuenta reactivada. " + motivoReactivacion,
                        request.getRemoteAddr()
                );

                JsonObject res = new JsonObject();
                res.addProperty("mensaje", "La cuenta fue reactivada correctamente.");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(res.toString());

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject err = new JsonObject();
                err.addProperty("error", "Acción no reconocida. Usa SUSPEND o REACTIVATE.");
                response.getWriter().print(err.toString());
            }

        } catch (SQLException e) {
            getServletContext().log("Error en Api_Security_Users doPut", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error interno al actualizar el estado del usuario.");
            response.getWriter().print(error.toString());
        }
    }
}
