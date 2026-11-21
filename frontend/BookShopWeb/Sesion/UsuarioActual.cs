using BookShopWeb.Code;
using BookShopWeb.Models;

namespace BookShopWeb.Sesion;

// Datos del usuario con sesión iniciada que muestran el encabezado y los menús (nombre, foto, correo)
public class UsuarioActual
{
    public Usuario? Datos { get; private set; }
    public event Action? Cambio;

    public async Task Cargar(int id)
    {
        if (Datos != null && Datos.Id == id) return;
        Datos = await new HttpClientUtils<Usuario>().Get($"usuarios/{id}");
        Cambio?.Invoke();
    }

    // Tras editar el perfil, el encabezado se actualiza sin recargar
    public void Actualizar(Usuario usuario)
    {
        Datos = usuario;
        Cambio?.Invoke();
    }
}
