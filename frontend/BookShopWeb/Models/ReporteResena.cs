namespace BookShopWeb.Models;

// Reporte de una reseña ajena, con la copia de su texto y su puntuación al reportarla
public class ReporteResena
{
    public int Id { get; set; }
    public Usuario Usuario { get; set; } = new();   // quien reporta
    public Resena Resena { get; set; } = new();
    public MotivoReporte Motivo { get; set; }
    public string Comentario { get; set; } = "";
    public string ContenidoReportado { get; set; } = "";
    public int PuntuacionReportada { get; set; }
    public EstadoSolicitud Estado { get; set; }
    public DateTime Fecha { get; set; }
}
