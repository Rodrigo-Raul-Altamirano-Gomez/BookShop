using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.1–2 y UX-UI §10.2: tabla de 20 filas, con búsqueda, estado y página en la URL
public partial class AdminLibros
{
    private const int TamanoPagina = 20;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [SupplyParameterFromQuery] public string? Q { get; set; }
    [SupplyParameterFromQuery] public string? Estado { get; set; }
    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private ConteoLibros? conteo;
    private ResultadoPagina<Libro>? resultado;
    private string busqueda = "";
    private string estado = "";
    private Libro? libroElegido;
    private DescuentoModal descuento = default!;
    private ConfirmModal confirmacion = default!;

    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        busqueda = Q ?? "";
        estado = Estado ?? "";
        await Recargar();
    }

    private async Task Recargar()
    {
        string consulta = $"libros/admin?pagina={Pagina ?? 1}&tamano={TamanoPagina}";
        if (busqueda != "") consulta += "&q=" + Uri.EscapeDataString(busqueda);
        if (estado != "") consulta += "&estado=" + estado;
        Task<ConteoLibros?> pedidoConteo = new HttpClientUtils<ConteoLibros>().Get("libros/conteo");
        Task<ResultadoPagina<Libro>?> pedidoLista = new HttpClientUtils<ResultadoPagina<Libro>>().Get(consulta);
        conteo = await pedidoConteo;
        resultado = await pedidoLista;
    }

    private void Filtrar()
    {
        IrA(1);
    }

    private void IrA(int pagina)
    {
        string url = "admin/libros?pagina=" + pagina;
        if (busqueda.Trim() != "") url += "&q=" + Uri.EscapeDataString(busqueda.Trim());
        if (estado != "") url += "&estado=" + estado;
        Navegacion.NavigateTo(url);
    }

    private static string Ruta(Libro libro) => "admin/libros/" + libro.Id;

    // "1–20 de 124"
    private string Rango()
    {
        int desde = ((Pagina ?? 1) - 1) * TamanoPagina + 1;
        int hasta = desde + resultado!.Elementos.Count - 1;
        return $"{desde}–{hasta} de {resultado.Total}";
    }

    private async Task PedirCambioEstado(Libro libro)
    {
        libroElegido = libro;
        if (libro.Activo)
        {
            await confirmacion.Mostrar($"¿Desactivar «{libro.Titulo}»?", "Dejará de mostrarse en la tienda. Lo ya registrado (pedidos, listas, reseñas) no cambia.", "Sí, desactivar", true);
        }
        else
        {
            await confirmacion.Mostrar($"¿Activar «{libro.Titulo}»?", "Volverá a mostrarse en la tienda.", "Sí, activar", false);
        }
    }

    // RF04.2 y RN03: se activa o desactiva, nunca se borra
    private async Task CambiarEstado()
    {
        string accion = libroElegido!.Activo ? "desactivar" : "activar";
        await new HttpClientUtils<object>().Put($"libros/{libroElegido.Id}/{accion}", new { });
        Aviso.Mostrar($"«{libroElegido.Titulo}» {(libroElegido.Activo ? "desactivado" : "activado")}");
        await Recargar();
    }
}
