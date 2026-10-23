document.getElementById("loginForm").addEventListener("submit", async function(event) {
    
    // 1. Evitar la recarga tradicional de la página
    event.preventDefault();

    // 2. Capturar el identificador (correo/celular) y la contraseña
    const datosLogin = {
        identifier: document.getElementById("inputIdentifier").value,
        password: document.getElementById("inputPassword").value
    };

    const alerta = document.getElementById("mensajeAlerta");

    try {
        // Detectar la ruta base del contexto (para Tomcat o NetBeans)
        const baseUrl = window.location.pathname.startsWith("/ECOAGRO")
            ? "/ECOAGRO/api/Api_Login"
            : "/api/Api_Login";

        // 3. Enviar la petición a la API
        const respuesta = await fetch(baseUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(datosLogin)
        });

        // 4. Extraer el mensaje JSON de la respuesta
        const resultado = await respuesta.json();

        // Limpiar estilos previos de la alerta
        alerta.classList.remove("d-none", "alert-danger", "alert-success");

        // 5. Evaluar el resultado según el código HTTP (200 OK)
        if (respuesta.ok) { 
            alerta.classList.add("alert-success");
            alerta.innerText = resultado.mensaje;
            
            // Redirigir según el rango asignado
            setTimeout(() => {
                if (resultado.rango === "SUPERADMINISTRADOR" || resultado.rango === "ADMIN_SEGURIDAD") {
                    window.location.href = "../users/user_admin/dashboard_admin.html";
                } else {
                    window.location.href = "../users/user_comprador/dashboard_comprador.html";
                }
            }, 800);

        } else { 
            // Mostrar error 400 (formato), 401 (credenciales) o 403 (cuenta suspendida)
            alerta.classList.add("alert-danger");
            if (resultado.motivo) {
                alerta.innerText = resultado.error + " (Motivo: " + resultado.motivo + ")";
            } else {
                alerta.innerText = resultado.error || "Error en el inicio de sesión.";
            }
        }

    } catch (error) {
        alerta.classList.remove("d-none");
        alerta.classList.add("alert-danger");
        alerta.innerText = "Error crítico de conexión con el servidor.";
        console.error("Error en Fetch:", error);
    }
});
