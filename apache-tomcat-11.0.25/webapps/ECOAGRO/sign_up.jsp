<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EcoAgro - Tu Mercado del Campo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
</head>
<body>
  <!-- Navbar -->
  <nav class="navbar_main navbar navbar-expand-md bg-body-tertiary">
    <div class="container">
      <!-- Logo de la empresa -->
          <a class="navbar-brand" href="index.jsp"><img src="imagenes/logo Ecoagro.jpg" alt="logo ecoagro" width="50" height="50" class="rounded-2"></a>
      <!-- Barra de bÃºsqueda -->
        <nav class="navbar bg-body-tertiary rounded-2 order-3 order-sm-0">
          <div class="container">
            <form class="d-flex" role="search">
              <input class="form-control me-2 rounded-2" type="search" placeholder="Search" aria-label="Search"/>
              <button class="btn btn-outline-success" type="submit"><img class="my-auto" src="imagenes/lupa.png" alt="Buscar" width="20" height="20"></button>
            </form>
          </div>
        </nav>
        <!-- BotÃ³n para colapsar el menÃº en pantallas pequeÃ±as -->
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNavAltMarkup" aria-controls="navbarNavAltMarkup" aria-expanded="false" aria-label="Toggle navigation">
          <img src="imagenes/icono-collapse.png" alt="menu" width="50" height="50"></button>
          </button>
      <!-- Enlaces de navegaciÃ³n -->
      <div class="collapse navbar-collapse" id="navbarNavAltMarkup">
        <ul class="navbar-nav ms-auto">
          <li class="nav-item">
            <a class="text_nav nav-link" href="products/catalogo_productos.html">catalogo</a>
          </li>
          <li class="nav-item">
            <a class="text_nav nav-link" href="login.jsp">login</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="products/shopping_cart.html"><img src="imagenes/carrito-de-compras.png" alt="carrito_de_compras" width="20" height="20"></a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="products/configuracion.html"><img src="imagenes/configuracion.png" alt="configuracion" width="20" height="20"></a>
          </li>
        </ul>
      </div>
      
    </div>
  </nav>

<div class="container d-flex justify-content-center align-items-center vh-100">  
    <div class="card shadow p-4" style="width: 100%; max-width: 400px;">
        <div class="card-body">
        <h3 class="text-center mb-4">registrate</h3>
            <% 
                String error = (String) request.getAttribute("errorAuth"); 
                if (error != null) { 
            %>
            <div class="alert alert-danger text-center mb-3" role="alert">
            <%= error %>
            </div>
        <% } %>
     <form action="sign_up" method="POST">
        <!-- Campo de Nombre con validación de solo letras y espacios -->
            <div class="mb-3">
                <label for="inputNombre" class="form-label">Nombre Completo</label>
                <input type="text" class="form-control" name="name" id="inputNombre" placeholder="Ingresa tu nombre" pattern="[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+" title="Solo se permiten letras y espacios, sin números ni caracteres especiales" required>
            </div>
        
        <!-- Campo de Correo -->
            <div class="mb-3">
                <label for="inputCorreo" class="form-label">Correo Electrónico</label>
                <input type="email" class="form-control" name="email" id="inputCorreo" placeholder="ejemplo@correo.com" required>
            </div>
        
        <!-- Campo de Celular/Teléfono con validación de solo números positivos -->
            <div class="mb-3">
                <label for="inputNumero" class="form-label">Celular / Teléfono</label>
                <input type="text" class="form-control" name="number" id="inputNumero" placeholder="3201234567" pattern="[0-9]+" title="Solo se permiten números positivos, sin letras ni signos" required>
            </div>
        
        <!-- Campo de Contraseña -->
            <div class="mb-4">
                <label for="inputPassword" class="form-label">Contraseña</label>
                <input type="password" class="form-control" name="password" id="inputPassword" placeholder="********" required>
            </div>
        
        <!-- Campo de Confirmar Contraseña -->
            <div class="mb-4">
                <label for="inputConfirmPassword" class="form-label">Confirmar Contraseña</label>
                <input type="password" class="form-control" name="confirm_password" id="inputConfirmPassword" placeholder="********" required>
            </div>
        
        <!-- Botón de Registrar -->
            <div class="d-grid">
                <button type="submit" class="btn_submit btn btn-primary">Registrarse</button>
            </div>
        </form>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
</body>
</html>