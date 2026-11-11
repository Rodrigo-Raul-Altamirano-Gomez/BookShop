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
                Tipo = CuentaServicio.TipoSeguimiento(Tipo),
                Orden = Orden == "reciente" ? "MAS_RECIENTES" : "NOMBRE_ASCENDENTE",
                Pagina = Pagina,
                TamanioPagina = TamanoPagina
            });
        Referencias = datos.Items.Select(dto => Presentacion.Referencia(dto, Tipo)).ToList();
        totalResultados = datos.TotalResultados;
    }

    private const string ClaveVista = "cuenta:mis-seguimientos";
    private string Tipo { get; set; } = "autores";
    private string Orden { get; set; } = "nombre";
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 12;
    private long totalResultados;
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + TamanoPagina - 1) / TamanoPagina));
    private List<Referencia> Referencias = [];

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue(ClaveVista, out var guardado)
            && guardado is VistaSeguimientos vista)
        {
            Tipo = vista.Tipo;
            Orden = vista.Orden;
            Pagina = vista.Pagina;
        }
    }

    private void GuardarVista() => Sesion.EstadoVistas[ClaveVista] = new VistaSeguimientos
    {
        Tipo = Tipo,
        Orden = Orden,
        Pagina = Pagina

    };
    private async Task MostrarAutores()
    {
        Tipo = "autores";
        Pagina = 1;
        GuardarVista();
        await ActualizarDatos();
    }

    private async Task MostrarCategorias()
    {
        Tipo = "categorias";
        Pagina = 1;
        GuardarVista();
        await ActualizarDatos();
    }

    private async Task MostrarEditoriales()
    {
        Tipo = "editoriales";
        Pagina = 1;
        GuardarVista();
        await ActualizarDatos();
    }

    private async Task ReiniciarPagina()
    {
        Pagina = 1;
        GuardarVista();
        await ActualizarDatos();
    }

    private async Task CambiarPagina(int pagina)
    {
        Pagina = pagina;
        GuardarVista();
        await ActualizarDatos();
    }

    private class VistaSeguimientos
    {
        public string Tipo { get; set; } = "autores";
        public string Orden { get; set; } = "nombre";
        public int Pagina { get; set; } = 1;
    }
}
