using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : IDisposable
{
    [Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id { get; set; }

    [Inject] private DocumentoServicio Documentos { get; set; } = default!;

    private Pedido? pedido;
    private bool confirmacion;
    private bool descargando;
    private string? errorComprobante;
    private string? urlComprobante;

    // RF17: el backend genera el PDF y la página lo muestra en una ventana, sin descargarlo.
    private async Task VerComprobante()
    {
        descargando = true;
        errorComprobante = null;
        try
        {
            urlComprobante = await Documentos.UrlComprobante(Id);
        }
        catch (InvalidOperationException e)
        {
            errorComprobante = e.Message;
        }
        catch (Microsoft.JSInterop.JSException)
        {
            errorComprobante = "El navegador no pudo mostrar el PDF.";
        }
        finally
        {
            descargando = false;
        }
    }

    private async Task CerrarComprobante()
    {
        if (urlComprobante is not null)
            await Documentos.LiberarUrl(urlComprobante);
        urlComprobante = null;
    }

    // RF17: comprobante en PDF generado por el backend.
    private async Task DescargarComprobante()
    {
        descargando = true;
        errorComprobante = null;
        try
        {
            await Documentos.DescargarComprobante(Id);
        }
        catch (InvalidOperationException e)
        {
            errorComprobante = e.Message;
        }
        catch (Microsoft.JSInterop.JSException)
        {
            errorComprobante = "El navegador no pudo guardar el archivo.";
        }
        finally
        {
            descargando = false;
        }
    }
    protected override void OnInitialized() => Sesion.Cambiado += Actualizar;
    protected override void OnParametersSet()
    {
        pedido = Compras.Pedidos.FirstOrDefault(o => o.Id == Id);
        confirmacion = Navegacion.Uri.Contains("/confirmacion/");
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}
