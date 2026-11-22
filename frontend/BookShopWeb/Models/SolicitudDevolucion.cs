namespace BookShopWeb.Models;

public class SolicitudDevolucion
{
    public int Id { get; set; }
    public Pedido Pedido { get; set; } = new();
    public MotivoDevolucion Motivo { get; set; }
    public string Comentario { get; set; } = "";
    public EstadoSolicitud Estado { get; set; }
    public DateTime Fecha { get; set; }
}
