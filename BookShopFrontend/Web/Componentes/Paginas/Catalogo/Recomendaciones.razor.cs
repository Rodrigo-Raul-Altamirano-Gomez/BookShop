using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Recomendaciones : IAsyncDisposable
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    [Inject] private SesionUsuario Sesion { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    private bool personal, cargando;
    private string? tipoCargado, correoCargado, idConsulta, error;
    private ConsultaRecomendacionesDTO consulta = new();
    private ResultadoPaginadoDTO<TarjetaLibroDTO> resultados = new();
    private string Titulo => personal ? "Para ti" : "Más populares";
    private int TotalPaginas => Math.Max(1, (int)((resultados.TotalResultados + 11) / 12));

    protected override void OnInitialized() => Sesion.Cambiado += ActualizarSesion;

    protected override async Task OnParametersSetAsync()
    {
        string tipo = new Uri(Navegacion.Uri).AbsolutePath;
        if (tipoCargado == tipo)
            return;
        tipoCargado = tipo;
        personal = tipo == "/para-ti";
        await Abrir();
    }

    private async Task Abrir()
    {
        cargando = true;
        error = null;
        resultados = new();
        consulta = new();
        correoCargado = Sesion.Usuario?.Correo;
        await Descartar();
        if (!personal || Sesion.Usuario is not null)
        {
            try
            {
                var datos = await Api.Post<RecomendacionesDTO>(
                    personal ? "recomendaciones/para-ti" : "recomendaciones/populares", consulta);
                idConsulta = datos.IdConsulta;
                resultados = datos.Resultados;
            }
            catch (InvalidOperationException e)
            {
                error = e.Message;
            }
        }
        cargando = false;
    }

    private async Task Consultar()
    {
        cargando = true;
        error = null;
        try
        {
            resultados = await Api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>(
                $"recomendaciones/consultas/{idConsulta}", consulta);
        }
        catch (InvalidOperationException e)
        {
            error = e.Message;
        }
        cargando = false;
    }

    private async Task Ordenar()
    {
        consulta.Pagina = 1;
        await Consultar();
    }

    private async Task CambiarPagina(int pagina)
    {
        consulta.Pagina = pagina;
        await Consultar();
    }

    private async void ActualizarSesion()
    {
        if (personal && correoCargado != Sesion.Usuario?.Correo)
            await InvokeAsync(async () => { await Abrir(); StateHasChanged(); });
    }

    private async Task Descartar()
    {
        if (idConsulta is null)
            return;
        string anterior = idConsulta;
        idConsulta = null;
        try
        {
            await Api.Delete($"recomendaciones/consultas/{anterior}");
        }
        catch (InvalidOperationException)
        {
            // Al cerrar la sesión puede haber terminado también el acceso a su consulta.
        }
    }

    public async ValueTask DisposeAsync()
    {
        Sesion.Cambiado -= ActualizarSesion;
        await Descartar();
    }
}
