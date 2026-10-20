using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisSolicitudes
{
    private static readonly string[] Estados = ["Pendiente", "Aceptada", "Rechazada", "Cerrada"];
    private readonly HashSet<string> EstadosPreparados = [];
    private readonly HashSet<string> EstadosAplicados = [];
    private string Tipo { get; set; } = "Devoluciones";
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 10;
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Solicitudes.Count / (double)TamanoPagina));
    private Solicitud? EnEdicion
    {
        get; set;
    }
    private Solicitud? EnConsulta
    {
        get; set;
    }
    private string Motivo { get; set; } = "";
    private string Comentario { get; set; } = "";
    private List<Solicitud> Solicitudes => Estado.Solicitudes.Where(solicitud => solicitud.Tipo == Tipo
        && (EstadosAplicados.Count == 0 || EstadosAplicados.Contains(solicitud.Estado))).OrderByDescending(
            solicitud => solicitud.FechaCreacion).ThenByDescending(solicitud => solicitud.Id).ToList();
    private IEnumerable<Solicitud> SolicitudesPagina => Solicitudes.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private Pedido? PedidoConsulta => Estado.Pedidos.FirstOrDefault(pedido => pedido.Id == EnConsulta?
        .IdReferencia);
    private Resena? ResenaConsulta => Estado.Resenas.FirstOrDefault(resena => resena.Id == EnConsulta?
        .IdReferencia);
    private string[] Motivos => EnEdicion?.Tipo == "Devoluciones" ? ["Libro dañado", "Libro incorrecto",
         "Pedido incompleto", "No cumple las expectativas", "Otro"] : ["Contenido ofensivo",
              "Publicidad/spam", "Contenido ajeno al libro", "Otro"];

    private static string Estrellas(int cantidad) => new('★', Math.Clamp(cantidad, 0, 5));
    private static string ClaseEstado(string estado) => estado switch
    {
        "Pendiente" => "warning",
        "Aceptada" => "success",
        "Rechazada" => "danger",
        _ => "neutral"
    };
    private static string ClaseEstadoPedido(string estado) => estado switch
    {
        "En almacén" => "status-amber",
        "En camino" => "status-blue",
        "Entregado" => "status-green",
        _ => "status-red"
    };
    private void MostrarDevoluciones()
    {
        Tipo = "Devoluciones";
        Pagina = 1;
    }

    private void MostrarReportes()
    {
        Tipo = "Reportes";
        Pagina = 1;
    }

    private void CambiarPagina(int pagina) => Pagina = pagina;
    private void SeleccionarEstado(string estado)
    {
        if (!EstadosPreparados.Add(estado))
            EstadosPreparados.Remove(estado);
    }

    private void AplicarFiltros()
    {
        EstadosAplicados.Clear();
        EstadosAplicados.UnionWith(EstadosPreparados);
        Pagina = 1;
    }

    private void LimpiarFiltros()
    {
        EstadosPreparados.Clear();
        EstadosAplicados.Clear();
        Pagina = 1;
    }

    private void QuitarEstado(string estado)
    {
        EstadosPreparados.Remove(estado);
        EstadosAplicados.Remove(estado);
        Pagina = 1;
    }

    private void Editar(Solicitud solicitud)
    {
        EnEdicion = solicitud;
        Motivo = solicitud.Motivo;
        Comentario = solicitud.Comentario;
    }

    private void CerrarEdicion() => EnEdicion = null;
    private void Guardar()
    {
        if (EnEdicion is null || Motivo.Length == 0)
            return;
        EnEdicion.Motivo = Motivo;
        EnEdicion.Comentario = Comentario.Trim();
        CerrarEdicion();
        Estado.Notificar();
        Estado.Avisar("Solicitud actualizada.");
    }

    private void Retirar(Solicitud solicitud)
    {
        Estado.Solicitudes.Remove(solicitud);
        Pagina = Math.Min(Pagina, TotalPaginas);
        Estado.Notificar();
        Estado.Avisar("Solicitud retirada.");
    }

    private void Consultar(Solicitud solicitud) => EnConsulta = solicitud;
    private void CerrarConsulta() => EnConsulta = null;
}

