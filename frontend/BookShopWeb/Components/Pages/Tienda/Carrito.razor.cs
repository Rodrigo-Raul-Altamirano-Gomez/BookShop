using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

// RF11 y UX-UI §8.10. Los importes los calcula el backend cada vez que cambia el carrito.
public partial class Carrito
{
    [Inject] private CarritoService CarritoSesion { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private int idUsuario;
    private ResumenCompra? resumen;
    private List<Libro>? complementarios;
    private bool actualizando;
    private ConfirmModal confirmacion = default!;

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        await CarritoSesion.Cargar();
        await Recalcular();
    }

    private async Task Recalcular()
    {
        actualizando = true;
        resumen = await new HttpClientUtils<ResumenCompra>().Post("pedidos/resumen",
            new SolicitudCompra(idUsuario, CarritoSesion.Items, null, null, null, null));
        // RF11.1: el backend deja fuera los libros inactivos o agotados y limita cada cantidad a su stock
        await CarritoSesion.Reemplazar(resumen!.Lineas);
        actualizando = false;
        // Las sugerencias dependen de los libros del carrito
        string ids = string.Join("&", CarritoSesion.Items.Select(i => "carrito=" + i.IdLibro));
        complementarios = (await new HttpClientUtils<ResultadoLibros>().Get("libros/complementarios?tamano=8&" + ids))!.Libros;
    }

    private async Task CambiarCantidad(LineaCompra linea, int cantidad)
    {
        await CarritoSesion.CambiarCantidad(linea.Libro.Id, cantidad);
        await Recalcular();
    }

    private async Task Quitar(LineaCompra linea)
    {
        await CarritoSesion.Quitar(linea.Libro.Id);
        Aviso.Mostrar("Libro quitado");
        await Recalcular();
    }

    private async Task PedirVaciar()
    {
        int libros = CarritoSesion.Cantidad;
        await confirmacion.Mostrar(mensaje: $"Se quitarán {(libros == 1 ? "el libro" : $"los {libros} libros")} del carrito.");
    }

    private async Task Vaciar()
    {
        await CarritoSesion.Vaciar();
        resumen = resumen! with { Lineas = new List<LineaCompra>() };
    }
}
