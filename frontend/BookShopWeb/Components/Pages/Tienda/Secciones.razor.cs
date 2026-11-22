using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

// Tipo: novedades, mas-vendidos, mas-populares, para-ti o complementarios; Similares llega con LibroId.
// El período significa "registro" (fecha de registro del libro) o "ventas" (ventas consideradas), ERS §2.3.
public partial class Secciones
{
    [Parameter] public string? Tipo { get; set; }
    [Parameter] public int? LibroId { get; set; }
    [Inject] private CarritoService Carrito { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private string? endpoint;
    private string titulo = "";
    private string explicacion = "";
    private string criterio = "";
    private string periodo = "ventas";
    private bool noEncontrada;

    protected override async Task OnParametersSetAsync()
    {
        endpoint = null;
        noEncontrada = false;
        var usuario = (await AuthState).User;

        if (LibroId != null)
        {
            Libro? libro = await new HttpClientUtils<Libro>().Get($"libros/{LibroId}");
            Mostrar($"libros/{LibroId}/similares", $"Similares a «{libro!.Titulo}»", "Libros con características parecidas.", "similitud", "registro");
        }
        else if (Tipo == "novedades")
        {
            Mostrar("libros/novedades", "Novedades", "Los libros agregados más recientemente al catálogo.", "fecha de registro", "registro");
        }
        else if (Tipo == "mas-vendidos")
        {
            Mostrar("libros/mas-vendidos", "Más vendidos", "Libros con más unidades vendidas en el período elegido.", "unidades vendidas", "ventas");
        }
        else if (Tipo == "mas-populares")
        {
            Mostrar("libros/mas-populares", "Más populares", "Combina ventas recientes, valoraciones y novedad.", "popularidad", "ventas");
        }
        else if (Tipo == "para-ti" && SesionUsuario.EsCliente(usuario))
        {
            Mostrar($"libros/para-ti/usuario/{SesionUsuario.Id(usuario)}", "Para ti", "Basado en lo que compran lectores con gustos parecidos a los tuyos.",
                "afinidad con tus compras", "ventas");
        }
        else if (Tipo == "complementarios" && SesionUsuario.EsCliente(usuario))
        {
            // Los libros del carrito de la sesión
            await Carrito.Cargar();
            string ids = string.Join("&", Carrito.Items.Select(i => "carrito=" + i.IdLibro));
            Mostrar("libros/complementarios?" + ids, "También suelen llevarse", "Libros que suelen comprarse junto con los de tu carrito.",
                "frecuencia de compra conjunta", "ventas");
        }
        else
        {
            noEncontrada = true;
        }
    }

    private void Mostrar(string endpointSeccion, string tituloSeccion, string explicacionSeccion, string criterioSeccion, string periodoSeccion)
    {
        endpoint = endpointSeccion;
        titulo = tituloSeccion;
        explicacion = explicacionSeccion;
        criterio = criterioSeccion;
        periodo = periodoSeccion;
    }
}
