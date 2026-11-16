using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisPedidos
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    private static readonly string[] Estados = ["EN_ALMACEN", "EN_CAMINO", "ENTREGADO", "CANCELADO", "DEVUELTO"];
    private FiltroPedidoDTO filtro = new();
    private List<string> estadosSeleccionados = [];
    private ResultadoPaginadoDTO<TarjetaPedidoDTO> resultado = new();
    private int TotalPaginas => Math.Max(1, (int)((resultado.TotalResultados + 9) / 10));

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue("cuenta:mis-pedidos", out var guardado))
            filtro = (FiltroPedidoDTO)guardado;
        Sesion.EstadoVistas["cuenta:mis-pedidos"] = filtro;
        estadosSeleccionados = filtro.Estados.ToList();
    }

    protected override async Task CargarDatos()
    {
        resultado = new();
        if (Sesion.Usuario is not null)
            resultado = await Api.Post<ResultadoPaginadoDTO<TarjetaPedidoDTO>>("pedidos/busqueda", filtro);
    }

    private void AlternarEstado(string estado)
    {
        if (estadosSeleccionados.Contains(estado))
            estadosSeleccionados.Remove(estado);
        else
            estadosSeleccionados.Add(estado);
    }

    private async Task Aplicar()
    {
        filtro.Estados = estadosSeleccionados.ToList();
        await Ordenar();
    }

    private async Task Limpiar()
    {
        estadosSeleccionados.Clear();
        filtro.Estados.Clear();
        filtro.Orden = "FECHA_PAGO_DESCENDENTE";
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
