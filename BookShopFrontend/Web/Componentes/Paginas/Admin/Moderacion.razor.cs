using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Admin;

public partial class Moderacion : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    private readonly string[] EstadosSolicitud = ["Pendiente", "Aceptada", "Rechazada", "Cerrada"];
    private HashSet<string> BorradorEstados = [], EstadosAplicados = [];
    private string Pestana = "Devoluciones";
    private int Pagina = 1;
    private const int TamanoPagina = 10;
    private Solicitud? SolicitudSeleccionada;
    private IEnumerable<Solicitud> Filtrados => Estado.Solicitudes.Where(r => r.Tipo == Pestana && (
        EstadosAplicados.Count == 0 || EstadosAplicados.Contains(r.Estado))).OrderByDescending(r => r
            .FechaCreacion);
    private IEnumerable<Solicitud> ElementosPagina => Filtrados.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Filtrados.Count() / (double)TamanoPagina));
    private Pedido? PedidoRelacionado => Estado.Pedidos.FirstOrDefault(o => o.Id == SolicitudSeleccionada?
        .IdReferencia);
    private Resena? ResenaRelacionada => Estado.Resenas.FirstOrDefault(r => r.Id == SolicitudSeleccionada?
        .IdReferencia);

    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    private void ActualizarVista() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private void AlternarEstado(string EstadoSolicitud)
    {
        if (!BorradorEstados.Add(EstadoSolicitud))
            BorradorEstados.Remove(EstadoSolicitud);
    }

    private void Aplicar()
    {
        EstadosAplicados = new(BorradorEstados);
        Pagina = 1;
    }

    private void Limpiar()
    {
        EstadosAplicados.Clear();
        BorradorEstados.Clear();
        Pagina = 1;
    }

    private void QuitarEstado(string EstadoSolicitud)
    {
        EstadosAplicados.Remove(EstadoSolicitud);
        BorradorEstados.Remove(EstadoSolicitud);
        Pagina = 1;
    }

    private void ElegirDevoluciones()
    {
        Pestana = "Devoluciones";
        Pagina = 1;
    }

    private void ElegirReportes()
    {
        Pestana = "Reportes";
        Pagina = 1;
    }

    private void CambiarPagina(int Siguiente) => Pagina = Siguiente;
    private void Abrir(Solicitud Solicitud) => SolicitudSeleccionada = Solicitud;
    private void Cerrar() => SolicitudSeleccionada = null;
    private string ClaseEstado(string EstadoSolicitud) => EstadoSolicitud switch
    {
        "Pendiente" => "status-amber",
        "Aceptada" => "status-green",
        "Rechazada" => "status-red",
        _ => ""
    };
    private string MostrarEstrellas(int Cantidad) => new string('★', Cantidad) + new string('☆',
         5 - Cantidad);
    private void ResolverAceptada() => Resolver("Aceptada");
    private void ResolverRechazada() => Resolver("Rechazada");
    private void ResolverCerrada() => Resolver("Cerrada");
    private void Resolver(string EstadoSolicitud)
    {
        if (SolicitudSeleccionada is null || SolicitudSeleccionada.Estado != "Pendiente")
            return;
        // Solo representa el resultado de la acción en la pantalla de demostración.
        SolicitudSeleccionada.Estado = EstadoSolicitud;
        SolicitudSeleccionada.FechaResolucion = new(2026, 10, 7, 12, 0, 0);
        Pagina = Math.Min(Pagina, TotalPaginas);
        Estado.Notificar();
        Estado.Avisar($"Solicitud {EstadoSolicitud.ToLower()}");
    }
}

