using BookShopWeb.Models;

namespace BookShopWeb.Code;

// Texto y color (StatusBadge, UX-UI §5.3) de los estados, y texto de los motivos
public static class Etiquetas
{
    public static string Estado(EstadoPedido estado) => estado switch
    {
        EstadoPedido.EN_ALMACEN => "En almacén",
        EstadoPedido.EN_CAMINO => "En camino",
        EstadoPedido.ENTREGADO => "Entregado",
        EstadoPedido.CANCELADO => "Cancelado",
        _ => "Devuelto"
    };

    public static string Tipo(EstadoPedido estado) => estado switch
    {
        EstadoPedido.EN_ALMACEN => "neutral",
        EstadoPedido.EN_CAMINO => "info",
        EstadoPedido.ENTREGADO => "ok",
        EstadoPedido.CANCELADO => "bad",
        _ => "warn"
    };

    public static string Estado(EstadoSolicitud estado) => estado switch
    {
        EstadoSolicitud.PENDIENTE => "Pendiente",
        EstadoSolicitud.ACEPTADA => "Aceptada",
        EstadoSolicitud.RECHAZADA => "Rechazada",
        _ => "Retirada"
    };

    public static string Tipo(EstadoSolicitud estado) => estado switch
    {
        EstadoSolicitud.PENDIENTE => "warn",
        EstadoSolicitud.ACEPTADA => "ok",
        EstadoSolicitud.RECHAZADA => "bad",
        _ => "neutral"
    };

    public static string Motivo(MotivoDevolucion motivo) => motivo switch
    {
        MotivoDevolucion.PRODUCTO_DANADO => "Producto dañado",
        MotivoDevolucion.PRODUCTO_EQUIVOCADO => "Producto equivocado",
        MotivoDevolucion.YA_NO_LO_QUIERO => "Ya no lo quiero",
        _ => "Otro"
    };

    public static string Motivo(MotivoReporte motivo) => motivo switch
    {
        MotivoReporte.OFENSIVA => "Ofensiva",
        MotivoReporte.SPAM => "Spam",
        MotivoReporte.NO_RELACIONADA => "No relacionada con el libro",
        _ => "Otro"
    };

    // Método de pago con su número enmascarado: "Tarjeta •••• •••• •••• 4821" o "Yape ••••• 4321"
    public static string MetodoPago(MetodoPago metodo)
    {
        string nombre = metodo.Tipo == TipoMetodoPago.TARJETA ? "Tarjeta" : metodo.Proveedor ?? "";
        return nombre + " " + Formato.Enmascarar(metodo.Numero);
    }
}
