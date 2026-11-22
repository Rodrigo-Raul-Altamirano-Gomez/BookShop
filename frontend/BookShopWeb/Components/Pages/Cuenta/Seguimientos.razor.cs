using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Cuenta;

// Pestaña (?tipo=autor|categoria|editorial), búsqueda, orden y página en la URL; páginas de 24
public partial class Seguimientos
{
    private const int TamanoPagina = 24;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    [SupplyParameterFromQuery] public string? Tipo { get; set; }
    [SupplyParameterFromQuery] public string? Q { get; set; }
    [SupplyParameterFromQuery] public string? Orden { get; set; }
    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private readonly TipoEntidad[] pestanas = { TipoEntidad.AUTOR, TipoEntidad.CATEGORIA, TipoEntidad.EDITORIAL };
    private int idUsuario;
    private TipoEntidad tipo;
    private ResultadoPagina<Entidad>? resultado;
    private string busqueda = "";
    private string orden = "nombre-asc";
    private int quitando;

    private string TextoBusqueda => "Buscar " + Entidades.Plural(tipo).ToLower() + " por nombre";
    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        tipo = Tipo switch
        {
            "categoria" => TipoEntidad.CATEGORIA,
            "editorial" => TipoEntidad.EDITORIAL,
            _ => TipoEntidad.AUTOR
        };
        busqueda = Q ?? "";
        orden = Orden ?? "nombre-asc";
        resultado = null;
        string consulta = $"seguimientos/usuario/{idUsuario}?tipo={tipo}&orden={orden}&pagina={Pagina ?? 1}&tamano={TamanoPagina}";
        if (busqueda != "")
        {
            consulta += "&q=" + Uri.EscapeDataString(busqueda);
        }
        resultado = await new HttpClientUtils<ResultadoPagina<Entidad>>().Get(consulta);
    }

    // Valor de la pestaña en la URL: autor, categoria o editorial
    private static string ValorUrl(TipoEntidad pestana) => Entidades.Filtro(pestana);

    // "No sigues a ningún autor.", "No sigues a ninguna categoría." o "No sigues a ninguna editorial."
    private string TituloVacio() => tipo == TipoEntidad.AUTOR
        ? "No sigues a ningún autor."
        : "No sigues a ninguna " + Entidades.Singular(tipo).ToLower() + ".";

    private async Task CambiarPagina(int pagina)
    {
        IrA(pagina);
        await JS.InvokeVoidAsync("irArriba");
    }

    // Buscar o cambiar el orden vuelve a la página 1
    private void IrA(int pagina)
    {
        string url = $"cuenta/seguimientos?tipo={ValorUrl(tipo)}&orden={orden}&pagina={pagina}";
        if (busqueda.Trim() != "")
        {
            url += "&q=" + Uri.EscapeDataString(busqueda.Trim());
        }
        Navegacion.NavigateTo(url);
    }

    // La tarjeta sale de la lista y se avisa (UX-UI §9.7)
    private async Task DejarDeSeguir(Entidad entidad)
    {
        quitando = entidad.Id;
        await new HttpClientUtils<object>().Delete($"seguimientos/usuario/{idUsuario}/entidad/{entidad.Id}");
        resultado!.Elementos.Remove(entidad);
        resultado = resultado with { Total = resultado.Total - 1 };
        quitando = 0;
        Aviso.Mostrar($"Dejaste de seguir a {entidad.Nombre}");
    }
}
