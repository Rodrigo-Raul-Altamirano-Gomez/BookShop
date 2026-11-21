namespace BookShopWeb.Models;

public class Configuracion
{
    public decimal CostoEnvio { get; set; }
    public int DiasDevolucion { get; set; }
    public int MaxSolicitudesDevolucion { get; set; }
    public int MaxCancelacionesMes { get; set; }
    public decimal CuponMontoMinimo { get; set; }
    public int CuponCadaPedidos { get; set; }
    public int CuponPorcentaje { get; set; }
    public int CuponDiasVigencia { get; set; }
}
