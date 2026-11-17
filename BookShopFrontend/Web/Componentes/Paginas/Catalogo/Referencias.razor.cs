using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Referencias : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";
    private string Titulo
    {
        get
        {
            if (Tipo == "autores")
                return "Autores";
            if (Tipo == "editoriales")
                return "Editoriales";
            return "Categorías";
        }
    }

    private string tipoAnterior = "";
    private VistaReferencias vista = new();
    private List<Referencia> Resultados = [];
    private long totalResultados;
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + 11) / 12));
    private async Task Consultar()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            var datos = await Api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>($"{Tipo}/busqueda",
                new FiltroReferenciaDTO
                {
                    Nombre = vista.Nombre,
                    SoloActivos = true,
                    Orden = vista.Orden == "reciente" ? "MAS_RECIENTES" : "NOMBRE_ASCENDENTE",
                    Pagina = vista.Pagina,
                    TamanioPagina = 12
                });
            Resultados = datos.Items.Select(dto => Presentacion.Referencia(dto, Tipo)).ToList();
            totalResultados = datos.TotalResultados;
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        cargando = false;
    }

    protected override async Task OnParametersSetAsync()
    {
        if (tipoAnterior != Tipo)
        {
            tipoAnterior = Tipo;
            string clave = $"referencias:{Tipo}";
            if (Sesion.EstadoVistas.ContainsKey(clave))
                vista = (VistaReferencias)Sesion.EstadoVistas[clave];
            else
            {
                vista = new VistaReferencias();
                Sesion.EstadoVistas[clave] = vista;
            }
        }
        Sesion.MostrarBusqueda(vista.Nombre, "Buscar " + Titulo.ToLowerInvariant(), BuscarPrincipal);
        await Consultar();
    }
    private async Task BuscarPrincipal(string texto)
    {
        vista.Nombre = texto.Trim();
        await InvokeAsync(async () => { await ActualizarResultados(); StateHasChanged(); });
    }
    public void Dispose()
    {
        if (Sesion.BuscarEnListado == BuscarPrincipal) Sesion.BuscarEnListado = null;
    }

    private class VistaReferencias
    {
        public string Nombre { get; set; } = "";
        public string Orden { get; set; } = "nombre";
        public int Pagina { get; set; } = 1;
    }

    private async Task ActualizarResultados()
    {
        vista.Pagina = 1;
        await Consultar();
    }
    private async Task CambiarPagina(int valor)
    {
        vista.Pagina = valor;
        await Consultar();
    }
}
