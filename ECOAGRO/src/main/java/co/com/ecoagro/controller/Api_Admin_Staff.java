package co.com.ecoagro.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import co.com.ecoagro.dao.SecurityLogDAO;
import co.com.ecoagro.dao.StaffDAO;
import co.com.ecoagro.security.StaffAuthorization;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// API para gestionar el personal administrativo (GET, POST, PUT, DELETE).
@WebServlet("/api/Api_Admin_Staff")
public class Api_Admin_Staff extends HttpServlet {

    // Leemos el cuerpo JSON de la solicitud.
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

    // Comprobamos si el usuario tiene sesión activa.
    private boolean sinSesion(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession sesion = request.getSession(false);

        if (sesion == null || sesion.getAttribute("idUsuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Debes iniciar sesion.");
            response.getWriter().print(error.toString());
            return true;
        }

        return false;
    }

    // Lista el personal administrativo.
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Comprobamos que el usuario haya iniciado sesión.
        if (sinSesion(request, response)) {
            return;
        }

        HttpSession sesion = request.getSession(false);
        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        try {
            // Revisamos si el usuario tiene permisos administrativos.
            boolean esSuperadmin      = StaffAuthorization.esSuperadministrador(idUsuario);
            boolean esAdminSeguridad  = StaffAuthorization.esAdministradorSeguridad(idUsuario);

            if (!esSuperadmin && !esAdminSeguridad) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JsonObject error = new JsonObject();
                error.addProperty("error", "No tienes permisos administrativos.");
                response.getWriter().print(error.toString());
                return;
            }

            // Consultamos el personal administrativo.
            StaffDAO dao = new StaffDAO();
            JsonArray personal = dao.listarPersonalAdministrativo();

            // Enviamos la respuesta con los datos.
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("mensaje", "Acceso administrativo autorizado.");
            respuesta.addProperty("idUsuario", idUsuario);
            respuesta.addProperty("nombre", (String) sesion.getAttribute("nombreUsuario"));
            respuesta.addProperty("email",  (String) sesion.getAttribute("emailUsuario"));
            respuesta.addProperty(
                    "rango",
                    esSuperadmin ? "SUPERADMINISTRADOR" : "ADMIN_SEGURIDAD"
            );
            respuesta.add("personal", personal);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error al listar el personal administrativo.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "No fue posible obtener la lista del personal.");
            response.getWriter().print(error.toString());
        }
    }

    // Asigna un rango a un usuario.
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Comprobamos que el usuario haya iniciado sesión.
        if (sinSesion(request, response)) {
            return;
        }

        HttpSession sesion = request.getSession(false);
        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        JsonObject respuesta = new JsonObject();

        try {
            // Verificamos permisos de superadministrador.
            if (!StaffAuthorization.esSuperadministrador(idUsuario)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                respuesta.addProperty(
                        "error",
                        "Solo el SUPERADMINISTRADOR puede asignar rangos."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Validamos los datos recibidos en el JSON.
            JsonObject cuerpo = leerCuerpoJson(request);

            if (cuerpo == null
                    || !cuerpo.has("idUser")
                    || !cuerpo.has("idRole")) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty(
                        "error",
                        "Se requiere idUser e idRole en el cuerpo JSON."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            int idUser;
            int idRole;

            try {
                idUser = cuerpo.get("idUser").getAsInt();
                idRole = cuerpo.get("idRole").getAsInt();
            } catch (NumberFormatException | UnsupportedOperationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty("error", "idUser e idRole deben ser numeros enteros.");
                response.getWriter().print(respuesta.toString());
                return;
            }

            StaffDAO dao = new StaffDAO();

            // Verificamos que el usuario exista.
            if (!dao.existeUsuario(idUser)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                respuesta.addProperty(
                        "error",
                        "No existe un usuario con el id " + idUser + "."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Verificamos que el rol exista.
            if (!dao.existeRol(idRole)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                respuesta.addProperty(
                        "error",
                        "No existe un rol con el id " + idRole + "."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Evitamos asignaciones duplicadas.
            if (dao.existeAsignacion(idUser)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                respuesta.addProperty(
                        "error",
                        "El usuario ya tiene un rango asignado. Usa PUT para cambiar el rango."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Guardamos la asignación del rango.
            dao.asignarRango(idUser, idRole);

            // Registro en auditoría
            new SecurityLogDAO().registrarEvento(
                    idUser,
                    idUsuario,
                    "ROLE_ASSIGNED",
                    "Se asignó el rol " + idRole + " al usuario " + idUser,
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_CREATED);
            respuesta.addProperty("mensaje", "Rango asignado correctamente.");
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error al asignar rango.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            respuesta.addProperty("error", "No fue posible asignar el rango.");
            response.getWriter().print(respuesta.toString());
        }
    }

    // Cambia el rango de una asignación existente.
    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Comprobamos que el usuario haya iniciado sesión.
        if (sinSesion(request, response)) {
            return;
        }

        HttpSession sesion = request.getSession(false);
        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        JsonObject respuesta = new JsonObject();

        try {
            // Verificamos permisos de superadministrador.
            if (!StaffAuthorization.esSuperadministrador(idUsuario)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                respuesta.addProperty(
                        "error",
                        "Solo el SUPERADMINISTRADOR puede cambiar rangos."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Validamos los datos recibidos en el JSON.
            JsonObject cuerpo = leerCuerpoJson(request);

            if (cuerpo == null
                    || !cuerpo.has("idAssignment")
                    || !cuerpo.has("idRole")) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty(
                        "error",
                        "Se requiere idAssignment e idRole en el cuerpo JSON."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            int idAssignment;
            int idRole;

            try {
                idAssignment = cuerpo.get("idAssignment").getAsInt();
                idRole       = cuerpo.get("idRole").getAsInt();
            } catch (NumberFormatException | UnsupportedOperationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty(
                        "error",
                        "idAssignment e idRole deben ser numeros enteros."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            StaffDAO dao = new StaffDAO();

            // Verificamos que la asignación exista.
            if (!dao.existeAsignacionPorId(idAssignment)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                respuesta.addProperty(
                        "error",
                        "No existe una asignacion con el id " + idAssignment + "."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Verificamos que el nuevo rol exista.
            if (!dao.existeRol(idRole)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                respuesta.addProperty(
                        "error",
                        "No existe un rol con el id " + idRole + "."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Protegemos al último superadministrador del sistema.
            String rangoActual = dao.obtenerNombreRolDeAsignacion(idAssignment);
            if ("SUPERADMINISTRADOR".equals(rangoActual)) {
                int totalSuperadmins = dao.contarSuperadministradores();
                if (totalSuperadmins <= 1) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    respuesta.addProperty(
                            "error",
                            "No puedes cambiar el rango del unico SUPERADMINISTRADOR del sistema."
                    );
                    response.getWriter().print(respuesta.toString());
                    return;
                }
            }

            // Actualizamos el rango de la asignación.
            dao.cambiarRango(idAssignment, idRole);

            // Registro en auditoría
            new SecurityLogDAO().registrarEvento(
                    null,
                    idUsuario,
                    "ROLE_MODIFIED",
                    "Se actualizó la asignación " + idAssignment + " al rol " + idRole,
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_OK);
            respuesta.addProperty("mensaje", "Rango actualizado correctamente.");
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error al cambiar el rango.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            respuesta.addProperty("error", "No fue posible actualizar el rango.");
            response.getWriter().print(respuesta.toString());
        }
    }

    // Retira la asignación de rango de un usuario.
    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Comprobamos que el usuario haya iniciado sesión.
        if (sinSesion(request, response)) {
            return;
        }

        HttpSession sesion = request.getSession(false);
        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        JsonObject respuesta = new JsonObject();

        try {
            // Verificamos permisos de superadministrador.
            if (!StaffAuthorization.esSuperadministrador(idUsuario)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                respuesta.addProperty(
                        "error",
                        "Solo el SUPERADMINISTRADOR puede retirar rangos."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Validamos los datos recibidos en el JSON.
            JsonObject cuerpo = leerCuerpoJson(request);

            if (cuerpo == null || !cuerpo.has("idAssignment")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty(
                        "error",
                        "Se requiere idAssignment en el cuerpo JSON."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            int idAssignment;

            try {
                idAssignment = cuerpo.get("idAssignment").getAsInt();
            } catch (NumberFormatException | UnsupportedOperationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                respuesta.addProperty("error", "idAssignment debe ser un numero entero.");
                response.getWriter().print(respuesta.toString());
                return;
            }

            StaffDAO dao = new StaffDAO();

            // Verificamos que la asignación exista.
            if (!dao.existeAsignacionPorId(idAssignment)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                respuesta.addProperty(
                        "error",
                        "No existe una asignacion con el id " + idAssignment + "."
                );
                response.getWriter().print(respuesta.toString());
                return;
            }

            // Protegemos al último superadministrador del sistema.
            String rangoActual = dao.obtenerNombreRolDeAsignacion(idAssignment);
            if ("SUPERADMINISTRADOR".equals(rangoActual)) {
                int totalSuperadmins = dao.contarSuperadministradores();
                if (totalSuperadmins <= 1) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    respuesta.addProperty(
                            "error",
                            "No puedes retirar al unico SUPERADMINISTRADOR del sistema."
                    );
                    response.getWriter().print(respuesta.toString());
                    return;
                }
            }

            // Eliminamos la asignación.
            dao.retirarAsignacion(idAssignment);

            // Registro en auditoría
            new SecurityLogDAO().registrarEvento(
                    null,
                    idUsuario,
                    "ROLE_REVOKED",
                    "Se retiró la asignación " + idAssignment,
                    request.getRemoteAddr()
            );

            response.setStatus(HttpServletResponse.SC_OK);
            respuesta.addProperty("mensaje", "Asignacion retirada correctamente.");
            response.getWriter().print(respuesta.toString());

        } catch (SQLException e) {
            getServletContext().log("Error al retirar la asignacion.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            respuesta.addProperty("error", "No fue posible retirar la asignacion.");
            response.getWriter().print(respuesta.toString());
        }
    }
}
