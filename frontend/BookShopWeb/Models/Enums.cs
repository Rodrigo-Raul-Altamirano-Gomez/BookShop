namespace BookShopWeb.Models;

public enum Rol { CLIENTE, ADMINISTRADOR }

public enum TipoMetodoPago { TARJETA, BILLETERA }

public enum TipoEntidad { AUTOR, CATEGORIA, EDITORIAL }

public enum EstadoDescuento { VIGENTE, PROGRAMADO, FINALIZADO }

public enum EstadoPedido { EN_ALMACEN, EN_CAMINO, ENTREGADO, CANCELADO, DEVUELTO }

public enum EstadoSolicitud { PENDIENTE, ACEPTADA, RECHAZADA, RETIRADA }

public enum MotivoDevolucion { PRODUCTO_DANADO, PRODUCTO_EQUIVOCADO, YA_NO_LO_QUIERO, OTRO }

public enum MotivoReporte { OFENSIVA, SPAM, NO_RELACIONADA, OTRO }

public enum TipoLista { DESEADOS, FAVORITOS }
