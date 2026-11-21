using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class Inicio
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;

    // Orden del panel Explorar (UX-UI §8.1)
    private readonly TipoEntidad[] tipos = { TipoEntidad.CATEGORIA, TipoEntidad.AUTOR, TipoEntidad.EDITORIAL };
    private List<Libro>? novedades;
    private readonly Dictionary<TipoEntidad, List<Entidad>> entidades = new();
    private string busqueda = "";

    protected override async Task OnInitializedAsync()
    {
        // RF07.3: secciones de 8 tarjetas. Las cuatro consultas se piden a la vez y cada bloque aparece cuando llega.
        List<Task> pedidos = new() { CargarNovedades() };
        foreach (TipoEntidad tipo in tipos)
        {
            pedidos.Add(CargarEntidades(tipo));
        }
        await Task.WhenAll(pedidos);
    }

    private async Task CargarNovedades()
    {
        ResultadoLibros? resultado = await new HttpClientUtils<ResultadoLibros>().Get("libros/novedades?tamano=8");
        novedades = resultado!.Libros;
        StateHasChanged();
    }

    private async Task CargarEntidades(TipoEntidad tipo)
    {
        ResultadoPagina<Entidad>? pagina = await new HttpClientUtils<ResultadoPagina<Entidad>>().Get($"entidades?tipo={tipo}&tamano=8");
        entidades[tipo] = pagina!.Elementos;
        StateHasChanged();
    }

    private void Buscar()
    {
        Navegacion.NavigateTo("catalogo?q=" + Uri.EscapeDataString(busqueda.Trim()));
    }
}
