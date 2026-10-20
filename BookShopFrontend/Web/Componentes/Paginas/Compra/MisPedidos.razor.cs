using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class MisPedidos : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    private static readonly string[] Estados = ["En almacén", "En camino", "Entregado", "Cancelado",
         "Devuelto"];
    private HashSet<string> preparados = [], aplicados = [];
    private int pagina = 1;
    private string orden = "fecha";
    private List<Pedido> Filtrados
    {
        get
        {
            var datos = Estado.Pedidos.Where(o => aplicados.Count == 0 || aplicados.Contains(o.Estado));
            return (orden switch
            {
                "menor" => datos.OrderBy(o => o.Total),
                "mayor" => datos.OrderByDescending(o => o.Total),
                _ => datos.OrderByDescending(o => o.FechaPago)
            }).ToList();
        }
    }

    protected override void OnInitialized()
    {
        if (Estado.EstadoVistas.TryGetValue("pedidos", out var value) && value is VistaPedidos vista)
        {
            preparados = [.. vista.Estados];
            aplicados = [.. vista.Estados];
            pagina = vista.Pagina;
            orden = vista.Orden;
        }

        Estado.Cambiado += Actualizar;
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void SeleccionarEstado(string estado)
    {
        if (!preparados.Add(estado))
            preparados.Remove(estado);
    }

    private void Aplicar()
    {
        aplicados = [.. preparados];
        pagina = 1;
    }

    private void Limpiar()
    {
        preparados.Clear();
        aplicados.Clear();
        pagina = 1;
    }

    private void QuitarEstado(string estado)
    {
        preparados.Remove(estado);
        aplicados.Remove(estado);
        pagina = 1;
    }

    private void ReiniciarPagina() => pagina = 1;
    private void CambiarPagina(int valor) => pagina = valor;
    private static string ClaseEstado(string estado) => estado switch
    {
        "En almacén" => "status-amber",
        "En camino" => "status-blue",
        "Entregado" => "status-green",
        _ => "status-red"
    };
    public void Dispose()
    {
        Estado.EstadoVistas["pedidos"] = new VistaPedidos([.. aplicados], pagina, orden);
        Estado.Cambiado -= Actualizar;
    }

    private record VistaPedidos(string[] Estados, int Pagina, string Orden);
}

