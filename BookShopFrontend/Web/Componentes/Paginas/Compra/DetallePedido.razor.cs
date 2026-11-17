using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    [Inject] private DocumentoServicio Documentos { get; set; } = default!;
    [Parameter] public int Id { get; set; }
    private DetallePedidoDTO? pedido;
    private int idCargado;
    private bool procesando, formularioCancelacion, formularioDevolucion;
    private DatosCancelacionDTO cancelacion = new();
    private string? errorCancelacion;
    private bool generandoComprobante;
    private string? errorComprobante;

    protected override async Task CargarDatos()
    {
        pedido = null;
        formularioCancelacion = formularioDevolucion = false;
        errorComprobante = null;
        if (Sesion.Usuario != null)
            pedido = await Api.Get<DetallePedidoDTO>($"pedidos/{Id}");
        idCargado = Id;
    }

    protected override async Task OnParametersSetAsync()
    {
        if (idCargado != Id)
            await ActualizarDatos();
    }

    private async Task ActualizarSeguimiento()
    {
        procesando = true;
        try
        {
            string estadoAnterior = pedido!.Pedido.Estado;
            pedido = await Api.Put<DetallePedidoDTO>($"pedidos/{Id}/seguimiento");
            Sesion.Avisar(pedido.Pedido.Estado == estadoAnterior
                ? "Aún no hay cambios en el seguimiento."
                : $"Pedido actualizado: {Presentacion.EstadoPedido(pedido.Pedido.Estado)}.");
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task AbrirCancelacion()
    {
        procesando = true;
        try
        {
            pedido = await Api.Get<DetallePedidoDTO>($"pedidos/{Id}");
            if (pedido.PuedeCancelar)
            {
                cancelacion = new();
                errorCancelacion = null;
                formularioCancelacion = true;
            }
            else
                Sesion.Avisar("Solo se puede cancelar un pedido En almacén.");
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task CancelarPedido()
    {
        procesando = true;
        errorCancelacion = null;
        try
        {
            pedido = await Api.Post<DetallePedidoDTO>($"pedidos/{Id}/cancelacion", cancelacion);
            formularioCancelacion = false;
            Sesion.Avisar($"Pedido cancelado. Reembolso simulado: {Presentacion.Dinero(pedido.Resumen.Total)}.");
        }
        catch (InvalidOperationException e)
        {
            errorCancelacion = e.Message;
        }
        procesando = false;
    }

    private async Task AbrirDevolucion()
    {
        procesando = true;
        try
        {
            await Api.Get<object>($"solicitudes/devoluciones/pedido/{Id}/permitido");
            formularioDevolucion = true;
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task EnviarDevolucion(DatosDevolucionDTO datos)
    {
        await Api.Post<object>($"solicitudes/devoluciones/pedido/{Id}", datos);
        formularioDevolucion = false;
        pedido = await Api.Get<DetallePedidoDTO>($"pedidos/{Id}");
        Sesion.Avisar("Solicitud de devolución enviada. Está pendiente de revisión.");
    }

    private async Task DescargarComprobante()
    {
        generandoComprobante = true;
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
            errorComprobante = "El navegador no pudo guardar el comprobante.";
        }
        generandoComprobante = false;
    }
}
