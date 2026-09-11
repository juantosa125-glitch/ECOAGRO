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
            <a class="text_nav nav-link" href="sign_up.jsp">crear cuenta</a>
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
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>

<div class="container d-flex justify-content-center align-items-center vh-100">        
    <div class="card shadow p-4" style="width: 100%; max-width: 400px;">
        <div class="card-body">
        <h3 class="text-center mb-4">Iniciar Sesión</h3>
            <% 
                String error = (String) request.getAttribute("errorAuth"); 
                if (error != null) { 
            %>
            <div class="alert alert-danger text-center mb-3" role="alert">
            <%= error %>
            </div>
        <% } %>
        <form action="login" method="POST">
        <!-- Campo combinado de Correo o Celular -->
            <div class="mb-3">
                <label for="inputIdentifier" class="form-label">Correo o Celular</label>
                <input type="text" class="form-control" name="identifier" id="inputIdentifier" placeholder="ejemplo@correo.com o 3201234567" pattern="[a-zA-Z0-9@._-]+" title="Ingresa tu correo o un número de celular sin espacios ni signos extraños" required>
            </div>
        
        <!-- Campo de Contraseña -->
            <div class="mb-4">
                <label for="inputPassword" class="form-label">Contraseña</label>
                <input type="password" class="form-control" name="password" id="inputPassword" placeholder="********" required>
            </div>
        
        <!-- Botón de Ingresar -->
            <div class="d-grid">
                <button type="submit" class="btn_submit btn btn-primary">Ingresar</button>
            </div>
        </form>
        </div>
    </div>
</div>
</body>
</html>