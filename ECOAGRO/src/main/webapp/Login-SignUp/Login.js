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
        // 3. Enviar la petición a la API (Ajusta /Login-SignUp/ si tu contexto de NetBeans es otro)
        const respuesta = await fetch("http://localhost:8080/api/Api_Login", {
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
            
            // Redirigir al dashboard tras un inicio de sesión exitoso
            setTimeout(() => {
                window.location.href = "../users/user_comprador/dashboard_comprador.html";
            }, 1000);

        } else { 
            // Mostrar error 400 (formato) o 401 (credenciales incorrectas)
            alerta.classList.add("alert-danger");
            alerta.innerText = resultado.error;
        }

    } catch (error) {
        alerta.classList.remove("d-none");
        alerta.classList.add("alert-danger");
        alerta.innerText = "Error crítico de conexión con el servidor.";
        console.error("Error en Fetch:", error);
    }
});