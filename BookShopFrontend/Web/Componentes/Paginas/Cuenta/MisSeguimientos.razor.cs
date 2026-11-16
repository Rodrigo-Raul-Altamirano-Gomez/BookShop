using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisSeguimientos
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario is null)
        {
            Referencias.Clear();
            totalResultados = 0;
            return;
        }
        var datos = await Api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>(
            "cuenta/seguimientos/busqueda", new ConsultaSeguimientosDTO
            {
                Nombre = vista.Nombre,
                Tipo = CuentaServicio.TipoSeguimiento(vista.Tipo),
                Orden = vista.Orden == "reciente" ? "MAS_RECIENTES" : "NOMBRE_ASCENDENTE",
                Pagina = vista.Pagina,
                TamanioPagina = TamanoPagina
            });
        Referencias = datos.Items.Select(dto => Presentacion.Referencia(dto, vista.Tipo)).ToList();
        totalResultados = datos.TotalResultados;
    }

    protected override void OnParametersSet() => MostrarBusqueda();

    private void MostrarBusqueda() => Sesion.MostrarBusqueda(vista.Nombre, "Buscar " + vista.Tipo, Buscar);
    private async Task Buscar(string texto)
    {
        vista.Nombre = texto;
        await InvokeAsync(async () => { await ReiniciarPagina(); StateHasChanged(); });
    }
    public override void Dispose()
    {
        if (Sesion.BuscarEnListado == Buscar) Sesion.BuscarEnListado = null;
        base.Dispose();
    }
    private const string ClaveVista = "cuenta:mis-seguimientos";
    private VistaSeguimientos vista = new();

    private const int TamanoPagina = 12;
    private long totalResultados;
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + TamanoPagina - 1) / TamanoPagina));
    private List<Referencia> Referencias = [];

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue(ClaveVista, out var guardado) && guardado is VistaSeguimientos almacenada)
            vista = almacenada;
        Sesion.EstadoVistas[ClaveVista] = vista;
    }

    private async Task MostrarTipo(string tipo)
    {
        vista.Tipo = tipo;
        vista.Nombre = "";
        MostrarBusqueda();
        await ReiniciarPagina();
    }
    private async Task ReiniciarPagina()
    {
        vista.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task CambiarPagina(int pagina)
    {
        vista.Pagina = pagina;
        await ActualizarDatos();
    }

    private class VistaSeguimientos
    {
        public string Nombre { get; set; } = "";
        public string Tipo { get; set; } = "autores";
        public string Orden { get; set; } = "nombre";
        public int Pagina { get; set; } = 1;
    }
}
