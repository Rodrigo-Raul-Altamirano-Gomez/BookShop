using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class Inicio
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    // Orden del panel Explorar (UX-UI §8.1)
    private readonly TipoEntidad[] tipos = { TipoEntidad.CATEGORIA, TipoEntidad.AUTOR, TipoEntidad.EDITORIAL };

    // Estantes de libros en su orden (UX-UI §8.1). Tipo: el de /secciones/{tipo}. Para ti se agrega solo para clientes.
    private readonly List<Estante> estantes = new()
    {
        new Estante("novedades", "Novedades", null, "libros/novedades"),
        new Estante("mas-vendidos", "Más vendidos", null, "libros/mas-vendidos"),
        new Estante("mas-populares", "Más populares", null, "libros/mas-populares")
    };

    private readonly Dictionary<string, List<Libro>> librosEstante = new();
    private readonly Dictionary<TipoEntidad, List<Entidad>> entidades = new();
    private string busqueda = "";

    protected override async Task OnInitializedAsync()
    {
        // RF13: Para ti, solo para clientes con historial suficiente (si no lo tiene, llega vacío y no se muestra)
        var usuario = (await AuthState).User;
        if (SesionUsuario.EsCliente(usuario))
        {
            estantes.Insert(0, new Estante("para-ti", "Para ti", "Basado en lo que compran lectores con gustos parecidos a los tuyos.",
                $"libros/para-ti/usuario/{SesionUsuario.Id(usuario)}"));
        }

        // RF07.3: secciones de 8 tarjetas. Todas las consultas se piden a la vez y cada bloque aparece cuando llega.
        List<Task> pedidos = new();
        foreach (Estante estante in estantes)
        {
            pedidos.Add(CargarEstante(estante));
        }
        foreach (TipoEntidad tipo in tipos)
        {
            pedidos.Add(CargarEntidades(tipo));
        }
        await Task.WhenAll(pedidos);
    }

    private async Task CargarEstante(Estante estante)
    {
        ResultadoLibros? resultado = await new HttpClientUtils<ResultadoLibros>().Get(estante.Endpoint + "?tamano=8");
        librosEstante[estante.Tipo] = resultado!.Libros;
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

    private record Estante(string Tipo, string Titulo, string? Explicacion, string Endpoint);
}
