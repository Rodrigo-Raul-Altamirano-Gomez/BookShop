using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id
    {
        get; set;
    }

    private Pedido? pedido;
    private bool confirmacion;
    private string? modal;
    private string motivo = "", comentario = "";
    private static readonly string[] MotivosDevolucion = ["Libro dañado", "Libro incorrecto",
         "Pedido incompleto", "No cumple las expectativas", "Otro"];
    protected override void OnInitialized() => Estado.Cambiado += Actualizar;
    protected override void OnParametersSet()
    {
        pedido = Estado.Pedidos.FirstOrDefault(o => o.Id == Id);
        confirmacion = Navegacion.Uri.Contains("/confirmacion/");
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void AbrirCancelacion()
    {
        motivo = "";
        modal = "cancelar";
    }

    private void AbrirDevolucion()
    {
        motivo = "";
        comentario = "";
        modal = "devolver";
    }

    private void AbrirComprobante() => modal = "comprobante";
    private void CerrarModal() => modal = null;
    private void ActualizarSeguimiento()
    {
        if (pedido is null)
            return;
        if (pedido.Estado == "En almacén")
        {
            pedido.Estado = "En camino";
            pedido.FechaSalida = DateTimeOffset.UtcNow.ToOffset(TimeSpan.FromHours(-5)).DateTime;
        }
        else if (pedido.Estado == "En camino")
        {
            pedido.Estado = "Entregado";
            pedido.FechaEntrega = DateTimeOffset.UtcNow.ToOffset(TimeSpan.FromHours(-5)).DateTime;
        }

        Estado.Avisar("Seguimiento actualizado.");
        Estado.Notificar();
    }

    private void CancelarPedido()
    {
        if (pedido is null)
            return;
        pedido.Estado = "Cancelado";
        modal = null;
        Estado.Avisar("Pedido cancelado. Reembolso simulado: " + EstadoDemostracion.Dinero(pedido.Total));
        Estado.Notificar();
    }

    private void SolicitarDevolucion()
    {
        if (pedido is null)
            return;
        Estado.Solicitudes.Add(new Solicitud
        {
            Id = Estado.Solicitudes.Select(r => r.Id).DefaultIfEmpty().Max() + 1,
            Tipo = "Devoluciones",
            IdReferencia = pedido.Id,
            Usuario = Estado.Usuario?.Nombre ?? "María López",
            Motivo = motivo,
            Comentario = comentario,
            FechaCreacion = DateTimeOffset.UtcNow.ToOffset(TimeSpan.FromHours(-5)).DateTime
        });
        modal = null;
        Estado.Avisar("Solicitud de devolución enviada.");
        Estado.Notificar();
    }

    private static string ClaseEstado(string estado) => estado switch
    {
        "En almacén" => "status-amber",
        "En camino" => "status-blue",
        "Entregado" => "status-green",
        _ => "status-red"
    };
    public void Dispose() => Estado.Cambiado -= Actualizar;
}

