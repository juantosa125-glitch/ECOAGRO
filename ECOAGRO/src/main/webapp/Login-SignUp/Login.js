/** Envía las credenciales del formulario y muestra el resultado del acceso. */
document.getElementById("loginForm").addEventListener("submit", async function(event) {
    
    event.preventDefault();

    const password = document.getElementById("inputPassword").value;
    const peticionLogin = {
        identifier: document.getElementById("inputIdentifier").value,
        password
    };

    const alerta = document.getElementById("mensajeAlerta");

    try {
        const respuesta = await fetch(event.currentTarget.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(peticionLogin)
        });

        const resultado = await respuesta.json();

        alerta.classList.remove("d-none", "alert-danger", "alert-success");

        if (respuesta.ok) { 
            alerta.classList.add("alert-success");
            alerta.innerText = resultado.mensaje;
            
            // Abre el panel del comprador tras mostrar la confirmación.
            setTimeout(() => {
                window.location.href = "../users/user_comprador/dashboard_comprador.html";
            }, 1000);

        } else { 
            alerta.classList.add("alert-danger");
            alerta.innerText = resultado.error;
        }

    } catch {
        alerta.classList.remove("d-none");
        alerta.classList.add("alert-danger");
        alerta.innerText = "Error crítico de conexión con el servidor.";
    }
});
