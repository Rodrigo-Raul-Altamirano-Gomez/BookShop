using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class LibroDetalle
{
    [Parameter] public int Id { get; set; }
    [Inject] private ComparacionService Comparacion { get; set; } = default!;
    [Inject] private CarritoService Carrito { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private Libro? libro;
    private bool noEncontrado;
    private bool conSesion;
    private bool esAdmin;
    private int cantidad = 1;

    // Al pasar de un libro a otro (enlaces del detalle), se vuelve a cargar
    protected override async Task OnParametersSetAsync()
    {
        var usuario = (await AuthState).User;
        conSesion = usuario.Identity?.IsAuthenticated == true;
        esAdmin = SesionUsuario.EsAdmin(usuario);
        libro = null;
        noEncontrado = false;
        cantidad = 1;
        try
        {
            libro = await new HttpClientUtils<Libro>().Get($"libros/{Id}");
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
        }
    }

    // RF09.5 y RF11.2: si ya está en el carrito, se suma la cantidad. El visitante va a iniciar sesión (UX-UI §3.3).
    private async Task AgregarAlCarrito()
    {
        if (!conSesion)
        {
            Navegacion.NavigateTo("login?aviso=1&volver=" + Uri.EscapeDataString("/libros/" + Id));
            return;
        }
        await Carrito.Agregar(libro!.Id, cantidad);
        Aviso.Mostrar("Agregado al carrito.", "Ver carrito", "carrito");
        cantidad = 1;
    }

    // RF09.7: cualquier usuario puede comparar
    private async Task Comparar()
    {
        await Comparacion.Agregar(libro!);
    }
}
