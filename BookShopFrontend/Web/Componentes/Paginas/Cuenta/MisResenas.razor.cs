using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisResenas
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    private ConsultaResenasDTO consulta = new("MAS_RECIENTES", 1, 10);
    private ResultadoPaginadoDTO<ResenaDTO> resultado = new();
    private ResenaDTO? resenaEnEdicion;
    private bool procesando;
    private int TotalPaginas => Math.Max(1, (int)((resultado.TotalResultados + 9) / 10));

    protected override async Task CargarDatos()
    {
        resultado = new();
        CerrarEdicion();
        if (Sesion.Usuario is not null)
            resultado = await Api.Post<ResultadoPaginadoDTO<ResenaDTO>>("resenas/propias/busqueda", consulta);
    }

    private async Task Ordenar()
    {
        consulta.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task CambiarPagina(int pagina)
    {
        consulta.Pagina = pagina;
        await ActualizarDatos();
    }

    private void CerrarEdicion() => resenaEnEdicion = null;

    private async Task Guardar(DatosResenaDTO datos)
    {
        await Api.Put($"resenas/{resenaEnEdicion!.Id}", datos);
        CerrarEdicion();
        await ActualizarDatos();
    }

    private async Task Eliminar(int id)
    {
        procesando = true;
        try
        {
            await Api.Delete($"resenas/{id}");
            if (resultado.Items.Count == 1 && consulta.Pagina > 1)
                consulta.Pagina--;
            await ActualizarDatos();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }
}
