using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    [Inject] private DocumentoServicio Documentos { get; set; } = default!;
    [Parameter] public int Id { get; set; }
    private DetallePedidoDTO? pedido;
    private bool descargando;
    private string? errorComprobante;
    private string? urlComprobante;
    private string EstadoPedido => pedido?.Pedido.Estado switch
    {
        "EN_ALMACEN" => "En almacén",
        "EN_CAMINO" => "En camino",
        "ENTREGADO" => "Entregado",
        "CANCELADO" => "Cancelado",
        "DEVUELTO" => "Devuelto",
        _ => ""
    };

    protected override async Task CargarDatos()
    {
        pedido = null;
        if (Sesion.Usuario is not null)
            pedido = await Api.Get<DetallePedidoDTO>($"pedidos/{Id}");
    }

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
}
