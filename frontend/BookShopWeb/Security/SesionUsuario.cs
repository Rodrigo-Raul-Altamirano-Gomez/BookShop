using System.Security.Claims;

namespace BookShopWeb.Security;

// Lectura de los datos de la cookie de sesión
public static class SesionUsuario
{
    public static int Id(ClaimsPrincipal usuario) => int.Parse(usuario.FindFirst(ClaimTypes.NameIdentifier)!.Value);

    public static bool EsCliente(ClaimsPrincipal usuario) => usuario.IsInRole("CLIENTE");

    public static bool EsAdmin(ClaimsPrincipal usuario) => usuario.IsInRole("ADMINISTRADOR");
}
