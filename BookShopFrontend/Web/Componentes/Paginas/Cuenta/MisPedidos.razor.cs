using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisPedidos
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    private static readonly string[] Estados = ["EN_ALMACEN", "EN_CAMINO", "ENTREGADO", "CANCELADO", "DEVUELTO"];
    private FiltroPedidoDTO filtro = new();
    private ResultadoPaginadoDTO<TarjetaPedidoDTO> resultado = new();
    private int TotalPaginas => Math.Max(1, (int)((resultado.TotalResultados + 9) / 10));

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.ContainsKey("cuenta:mis-pedidos"))
            filtro = (FiltroPedidoDTO)Sesion.EstadoVistas["cuenta:mis-pedidos"];
        Sesion.EstadoVistas["cuenta:mis-pedidos"] = filtro;
    }

    protected override async Task CargarDatos()
    {
        resultado = new();
        if (Sesion.Usuario != null)
            resultado = await Api.Post<ResultadoPaginadoDTO<TarjetaPedidoDTO>>("pedidos/busqueda", filtro);
    }

    private async Task AlternarEstado(string estado)
    {
        if (filtro.Estados.Contains(estado))
            filtro.Estados.Remove(estado);
        else
            filtro.Estados.Add(estado);
        await Ordenar();
    }

    private async Task Ordenar()
    {
        filtro.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task CambiarPagina(int pagina)
    {
        filtro.Pagina = pagina;
        await ActualizarDatos();
    }
}
