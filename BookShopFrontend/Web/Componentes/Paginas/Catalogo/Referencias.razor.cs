using Microsoft.AspNetCore.Components;
using Web.Servicios;
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
            if (Tipo == "autores") return "Autores";
            if (Tipo == "editoriales") return "Editoriales";
            return "Categorías";
        }
    }

    private string orden = "nombre", tipoAnterior = "";
    private int pagina = 1;
    private List<Referencia> Resultados = [];
    private long totalResultados;
    private int TotalPaginas => Math.Max(1, (int)((totalResultados + 11) / 12));
    private IEnumerable<Referencia> ReferenciasVisibles => Resultados;
    private async Task Consultar()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            var datos = await Api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>($"{Tipo}/busqueda",
                new FiltroReferenciaDTO
                {
                    Nombre = "",
                    SoloActivos = true,
                    Orden = orden == "reciente" ? "MAS_RECIENTES" : "NOMBRE_ASCENDENTE",
                    Pagina = pagina,
                    TamanioPagina = 12
                });
            Resultados = datos.Items.Select(dto => Presentacion.Referencia(dto, Tipo)).ToList();
            totalResultados = datos.TotalResultados;
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }

    protected override async Task OnParametersSetAsync()
    {
        if (tipoAnterior != Tipo)
        {
            GuardarVista();
            tipoAnterior = Tipo;
            if (Sesion.EstadoVistas.TryGetValue($"referencias:{Tipo}", out var guardado)
                && guardado is VistaReferencias vista)
            {
                orden = vista.Orden;
                pagina = vista.Pagina;
            }
            else
            {
                orden = "nombre";
                pagina = 1;
            }
        }

        await Consultar();
    }

    private void GuardarVista()
    {
        if (tipoAnterior.Length == 0)
            return;
        Sesion.EstadoVistas[$"referencias:{tipoAnterior}"] = new VistaReferencias
        {
            Orden = orden,
            Pagina = pagina

        };
    }

    public void Dispose() => GuardarVista();
    private class VistaReferencias
    {
        public string Orden { get; set; } = "nombre";
        public int Pagina { get; set; } = 1;
    }

    private async Task CambiarOrden()
    {
        pagina = 1;
        await Consultar();
    }
    private async Task CambiarPagina(int valor)
    {
        pagina = valor;
        await Consultar();
    }
}
