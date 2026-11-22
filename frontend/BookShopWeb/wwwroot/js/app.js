// JavaScript propio: solo lo que Blazor no resuelve por sí mismo

// Diálogos de Bootstrap: atrapan el foco, cierran con Esc y lo devuelven al cerrar (UX-UI §13)
function abrirModal(id) {
    bootstrap.Modal.getOrCreateInstance(document.getElementById(id)).show();
}

function cerrarModal(id) {
    const modal = bootstrap.Modal.getInstance(document.getElementById(id));
    if (modal) modal.hide();
}

// Foco en un elemento: el primer campo con error (UX-UI §6.9) o el primero al pasar a edición (UX-UI §7.1)
function enfocar(selector) {
    setTimeout(() => {
        const el = document.querySelector(selector);
        if (el) el.focus();
    }, 50);
}

// Al cambiar de página en una vista de resultados, la vista vuelve arriba (UX-UI §6.7)
function irArriba() {
    window.scrollTo({ top: 0 });
}

// Datos de la sesión del navegador (comparación y carrito).
// Es una cookie sin fecha de expiración: la comparten las pestañas y se borra al cerrar el navegador.
function guardarSesion(nombre, valor) {
    document.cookie = nombre + "=" + encodeURIComponent(valor) + "; path=/; SameSite=Lax";
}

function leerSesion(nombre) {
    const parte = document.cookie.split("; ").find(c => c.startsWith(nombre + "="));
    return parte ? decodeURIComponent(parte.substring(nombre.length + 1)) : null;
}

// Envía las credenciales ya validadas a /auth/login, que firma la cookie de sesión y redirige
function iniciarSesion(correo, contrasena, volver) {
    const form = document.createElement("form");
    form.method = "post";
    form.action = "/auth/login";
    for (const [nombre, valor] of Object.entries({ correo, contrasena, volver })) {
        const campo = document.createElement("input");
        campo.type = "hidden";
        campo.name = nombre;
        campo.value = valor || "";
        form.appendChild(campo);
    }
    document.body.appendChild(form);
    form.submit();
}
