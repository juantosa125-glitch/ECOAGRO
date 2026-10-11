// Escuchar cuando el usuario presiona el botón de "Registrarse"
document.getElementById("registroForm").addEventListener("submit", async function(event) {
    
    // Evita que la página HTML haga un refresh automático
    event.preventDefault();

    // Empaquetar los datos de los inputs en un objeto
    const datosUsuario = {
        name: document.getElementById("inputNombre").value,
        email: document.getElementById("inputCorreo").value,
        number: document.getElementById("inputNumero").value,
        password: document.getElementById("inputPassword").value,
        confirm_password: document.getElementById("inputConfirmPassword").value
    };

    // Seleccionamos el div donde mostraremos el mensaje
    const alerta = document.getElementById("mensajeAlerta");

    try {
        // Hacer la petición POST a tu API usando fetch
        const respuesta = await fetch("http://localhost:8080/api/Api_Sign_Up", {
            method: "POST",
            headers: {
                "Content-Type": "application/json" 
            },
            body: JSON.stringify(datosUsuario) 
        });

        // Leer la respuesta JSON enviada por Java
        const resultado = await respuesta.json();

        // Mostrar la alerta dependiendo del código de estado HTTP
        alerta.classList.remove("d-none", "alert-danger", "alert-success");

        if (respuesta.ok) { // Éxito (200 o 201)
            alerta.classList.add("alert-success");
            alerta.innerText = resultado.mensaje;
            
            // Limpiar el formulario y redirigir
            document.getElementById("registroForm").reset();
            setTimeout(() => window.location.href = "login.jsp", 2000);

        } else { // Errores de validación o base de datos (400, 500)
            alerta.classList.add("alert-danger");
            alerta.innerText = resultado.error;
        }

    } catch (error) {
        alerta.classList.remove("d-none");
        alerta.classList.add("alert-danger");
        alerta.innerText = "Error de conexión con el servidor.";
        console.error(error);
    }
});