namespace Web.Modelos;
// Datos de presentación para recorrer las pantallas. No representan entidades persistentes.
public class Libro
{
    public int Id
    {
        get; set;
    }
    public string Titulo { get; set; } = "";
    public string Autor { get; set; } = "";
    public string Categoria { get; set; } = "";
    public string Editorial { get; set; } = "";
    public string Proveedor { get; set; } = "Distribuidora Andina";
    public string Idioma { get; set; } = "Español";
    public string Sinopsis { get; set; } = "";
    public string ISBN { get; set; } = "";
    public decimal Precio
    {
        get; set;
    }
    public decimal PrecioOriginal
    {
        get; set;
    }
    public int Descuento
    {
        get; set;
    }
    public int Stock { get; set; } = 12;
    public int Paginas { get; set; } = 320;
    public int Anio { get; set; } = 2024;
    public string Peso { get; set; } = "0.35 kg";
    public string Dimensiones { get; set; } = "21 × 14 cm";
    public string Saga { get; set; } = "";
    public int? Tomo
    {
        get; set;
    }
    public string Portada { get; set; } = "/images/book-placeholder.svg";
    public bool Activo { get; set; } = true;
    public List<DescuentoLibro> Descuentos { get; set; } = [];
}

public class DescuentoLibro
{
    public int Porcentaje { get; set; } = 15;
    public DateTime Inicio { get; set; } = new(2026, 10, 1);
    public DateTime Fin { get; set; } = new(2026, 10, 31);
}

public class Referencia
{
    public int Id
    {
        get; set;
    }
    public string Tipo { get; set; } = "categorias";
    public string Nombre { get; set; } = "";
    public string Descripcion { get; set; } = "";
    public string Imagen { get; set; } = "/images/reference-placeholder.svg";
    public bool Activo { get; set; } = true;
}

public class Perfil
{
    public string Nombre { get; set; } = "María López";
    public string Correo { get; set; } = "maria@correo.pe";
    public DateOnly Nacimiento { get; set; } = new(1998, 6, 12);
    public string Imagen { get; set; } = "/images/user-placeholder.svg";
    public bool EsAdministrador
    {
        get; set;
    }
}

public class Resena
{
    public int Id
    {
        get; set;
    }
    public int IdLibro
    {
        get; set;
    }
    public string Usuario { get; set; } = "María López";
    public string FotoUsuario { get; set; } = "/images/user-placeholder.svg";
    public DateTime Fecha { get; set; } = new(2026, 10, 3);
    public int Puntuacion { get; set; } = 5;
    public string Comentario { get; set; } = "";
    public int Votos
    {
        get; set;
    }
    public bool Propia
    {
        get; set;
    }
    public bool Votada
    {
        get; set;
    }
    public bool Activo { get; set; } = true;
}

public class Direccion
{
    public int Id
    {
        get; set;
    }
    public string Alias { get; set; } = "";
    public string Calle { get; set; } = "";
    public string Distrito { get; set; } = "";
    public string Ciudad { get; set; } = "Lima";
    public string ReferenciaAdicional { get; set; } = "";
    public bool Predeterminada
    {
        get; set;
    }
}

public class MetodoPago
{
    public int Id
    {
        get; set;
    }
    public string Tipo { get; set; } = "Tarjeta";
    public string Alias { get; set; } = "";
    public string Titular { get; set; } = "María López";
    public string Proveedor { get; set; } = "";
    public string TipoTarjeta { get; set; } = "Débito";
    public string NumeroEnmascarado { get; set; } = "•••• •••• •••• 4242";
    public DateOnly Vencimiento { get; set; } = new(2028, 12, 31);
    public bool Predeterminada
    {
        get; set;
    }
}

public class Cupon
{
    public int Id
    {
        get; set;
    }
    public int Porcentaje { get; set; } = 10;
    public decimal Minimo { get; set; } = 100;
    public DateOnly Vencimiento { get; set; } = new(2026, 10, 31);
    public string Estado { get; set; } = "Disponible";
}

public class LineaPedido
{
    public int IdLibro
    {
        get; set;
    }
    public int Cantidad
    {
        get; set;
    }
    public decimal PrecioOriginal
    {
        get; set;
    }
    public decimal Precio
    {
        get; set;
    }
    public int Descuento
    {
        get; set;
    }
}

public class Pedido
{
    public int Id
    {
        get; set;
    }
    public DateTime FechaPago { get; set; } = new(2026, 10, 5, 10, 30, 0);
    public string Estado { get; set; } = "En almacén";
    public List<LineaPedido> Lineas { get; set; } = [];
    public Direccion Direccion { get; set; } = new();
    public MetodoPago Metodo { get; set; } = new();
    public decimal Productos
    {
        get; set;
    }
    public decimal DescuentoLibro
    {
        get; set;
    }
    public decimal Subtotal
    {
        get; set;
    }
    public decimal DescuentoCupon
    {
        get; set;
    }
    public decimal Envio
    {
        get; set;
    }
    public decimal Total
    {
        get; set;
    }
    public string Transaccion { get; set; } = "SIM-20261005-0042";
    public string Seguimiento { get; set; } = "BS-PE-0042";
    public DateTime EntregaEstimada { get; set; } = new(2026, 10, 5, 12, 0, 0);
    public DateTime? FechaSalida
    {
        get; set;
    }
    public DateTime? FechaEntrega
    {
        get; set;
    }
}

public class Solicitud
{
    public int Id
    {
        get; set;
    }
    public string Tipo { get; set; } = "Devoluciones";
    public int IdReferencia
    {
        get; set;
    }
    public string Usuario { get; set; } = "María López";
    public DateTime FechaCreacion { get; set; } = new(2026, 10, 5, 14, 0, 0);
    public string Motivo { get; set; } = "Libro dañado";
    public string Comentario { get; set; } = "";
    public string Estado { get; set; } = "Pendiente";
    public DateTime? FechaResolucion
    {
        get; set;
    }
    public string ComentarioOriginal { get; set; } = "";
    public int PuntuacionOriginal { get; set; } = 3;
}

