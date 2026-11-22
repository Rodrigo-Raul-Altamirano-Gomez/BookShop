using BookShopWeb.Code;
using BookShopWeb.Models;
using BookShopWeb.Security;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Cuenta;

// RF03.1–2 y UX-UI §9.2: estado, orden y página en la URL; tabla de 20 filas
public partial class Pedidos
{
    private const int TamanoPagina = 20;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    [SupplyParameterFromQuery] public string? Estado { get; set; }
    [SupplyParameterFromQuery] public string? Orden { get; set; }
    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private ResultadoPagina<Pedido>? resultado;
    private bool sinPedidos;

    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        int idUsuario = SesionUsuario.Id((await AuthState).User);
        string consulta = $"pedidos/usuario/{idUsuario}?pagina={Pagina ?? 1}&tamano={TamanoPagina}";
        if (Estado != null) consulta += "&estado=" + Estado;
        if (Orden != null) consulta += "&orden=" + Orden;
        resultado = await new HttpClientUtils<ResultadoPagina<Pedido>>().Get(consulta);
        sinPedidos = Estado == null && resultado!.Total == 0;
    }

    private void Abrir(Pedido pedido)
    {
        Navegacion.NavigateTo($"cuenta/pedidos/{pedido.Id}");
    }

    private void CambiarOrden(ChangeEventArgs e)
    {
        Navegacion.NavigateTo(Url(Estado, e.Value?.ToString(), 1));
    }

    private static string Url(string? estado, string? orden, int pagina)
    {
        List<string> partes = new();
        if (estado != null) partes.Add("estado=" + estado);
        if (orden != null && orden != "fecha-desc") partes.Add("orden=" + orden);
        if (pagina > 1) partes.Add("pagina=" + pagina);
        return "cuenta/pedidos" + (partes.Count > 0 ? "?" + string.Join("&", partes) : "");
    }

    // "1–20 de 36"
    private string Rango()
    {
        int desde = ((Pagina ?? 1) - 1) * TamanoPagina + 1;
        int hasta = desde + resultado!.Elementos.Count - 1;
        return $"{desde}–{hasta} de {resultado.Total}";
    }
}
