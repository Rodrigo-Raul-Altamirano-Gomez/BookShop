using System.Security.Claims;
using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;

namespace BookShopWeb.Security;

public static class AuthEndpoints
{
    public static IEndpointRouteBuilder MapAuthEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // Recibe el formulario de inicio de sesión, valida con el backend y firma la cookie
        endpoints.MapPost("/auth/login", async (HttpContext context) =>
        {
            var form = await context.Request.ReadFormAsync();
            string volver = form["volver"].ToString();
            Usuario? usuario;
            try
            {
                usuario = await new HttpClientUtils<Usuario>().Post("usuarios/login",
                    new Credenciales(form["correo"].ToString(), form["contrasena"].ToString()));
            }
            catch (ApiException)
            {
                return Results.LocalRedirect("/login?error=1");
            }

            var claims = new List<Claim>
            {
                new(ClaimTypes.NameIdentifier, usuario!.Id.ToString()),
                new(ClaimTypes.Name, usuario.NombreCompleto),
                new(ClaimTypes.Email, usuario.Correo),
                new(ClaimTypes.Role, usuario.Rol.ToString())
            };
            var principal = new ClaimsPrincipal(new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme));
            await context.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, principal);

            // UX-UI §4.6: el administrador llega a Libros; el cliente vuelve a la página indicada o al inicio
            if (usuario.Rol == Rol.ADMINISTRADOR)
            {
                return Results.LocalRedirect("/admin/libros");
            }
            if (volver.StartsWith('/') && !volver.StartsWith("//"))
            {
                return Results.LocalRedirect(volver);
            }
            return Results.LocalRedirect("/");
        });

        endpoints.MapGet("/auth/logout", async (HttpContext context) =>
        {
            await context.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            // El carrito vive en la sesión: se pierde al cerrarla (ERS §2.1)
            context.Response.Cookies.Delete("bk-carrito");
            return Results.LocalRedirect("/");
        });

        return endpoints;
    }
}
