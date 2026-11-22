namespace BookShopWeb.Models;

// DTO que devuelve o recibe el backend (no son tablas)

public record ResultadoLibros
{
    public int Total { get; init; }
    public int Pagina { get; init; }
    public int TotalPaginas { get; init; }
    public List<Libro> Libros { get; init; } = new();
    public List<Entidad> Categorias { get; init; } = new();
    public List<Entidad> Autores { get; init; } = new();
    public List<Entidad> Editoriales { get; init; } = new();
    public List<string> Idiomas { get; init; } = new();
}

public record ResultadoPagina<T>
{
    public int Total { get; init; }
    public List<T> Elementos { get; init; } = new();
}

public record EntidadAdmin
{
    public Entidad Entidad { get; init; } = new();
    public int CantidadLibros { get; init; }
}

public record ConteoLibros
{
    public int Total { get; init; }
    public int Activos { get; init; }
}

public record Credenciales(string Correo, string Contrasena);

// Un libro del carrito de la sesión
public record ItemCarrito(int IdLibro, int Cantidad);

public record LineaCompra
{
    public Libro Libro { get; init; } = new();
    public int Cantidad { get; init; }
    public decimal Subtotal { get; init; }
}

// Importes del carrito y del resumen de compra; Cupon es null si no se aplica ninguno
public record ResumenCompra
{
    public List<LineaCompra> Lineas { get; init; } = new();
    public Cupon? Cupon { get; init; }
    public decimal Subtotal { get; init; }
    public decimal DescuentoCupon { get; init; }
    public decimal CostoEnvio { get; init; }
    public decimal Total { get; init; }
}

// Para calcular el resumen basta con Items e IdCupon; para pagar va todo, con el resumen que vio el cliente
public record SolicitudCompra(int IdUsuario, List<ItemCarrito> Items, int? IdDireccion, int? IdMetodoPago, int? IdCupon, ResumenCompra? Preparado);

// Reseñas del detalle de un libro: la propia primero; el resumen cuenta solo las visibles.
// Distribucion: cantidad de reseñas de 5, 4, 3, 2 y 1 estrellas.
public record ResenasLibro
{
    public List<Resena> Resenas { get; init; } = new();
    public double Promedio { get; init; }
    public int Cantidad { get; init; }
    public List<int> Distribucion { get; init; } = new();
    public bool PuedeResenar { get; init; }
}

// Detalle de un pedido con lo que decide sus acciones (cancelar y devolver)
public record PedidoDetalle
{
    public Pedido Pedido { get; init; } = new();
    public int CancelacionesRestantes { get; init; }
    public DateOnly? DevolucionHasta { get; init; }
    public bool DevolucionEnPlazo { get; init; }
    public bool DevolucionPendiente { get; init; }
    public int SolicitudesRestantes { get; init; }
}

public record CambioContrasena(string Actual, string Nueva);
