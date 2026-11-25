/** Envía la solicitud de registro y muestra el resultado del alta. */
document.getElementById("registroForm").addEventListener("submit", async function(event) {
    
    event.preventDefault();

    const password = document.getElementById("inputPassword").value;
    const confirmPassword = document.getElementById("inputConfirmPassword").value;
    const peticionRegistro = {
        name: document.getElementById("inputNombre").value,
        email: document.getElementById("inputCorreo").value,
        number: document.getElementById("inputNumero").value,
        password,
        confirm_password: confirmPassword
    };

    const alerta = document.getElementById("mensajeAlerta");

    try {
        const respuesta = await fetch(event.currentTarget.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/json" 
            },
            body: JSON.stringify(peticionRegistro)
        });

        const resultado = await respuesta.json();

        alerta.classList.remove("d-none", "alert-danger", "alert-success");

        if (respuesta.ok) {
            alerta.classList.add("alert-success");
            alerta.innerText = resultado.mensaje;
            
            document.getElementById("registroForm").reset();
            // Abre el inicio de sesión tras mostrar la confirmación del registro.
            setTimeout(() => window.location.href = "login.jsp", 2000);

        } else {
            alerta.classList.add("alert-danger");
            alerta.innerText = resultado.error;
        }

    } catch {
        alerta.classList.remove("d-none");
        alerta.classList.add("alert-danger");
        alerta.innerText = "Error de conexión con el servidor.";
    }
});
