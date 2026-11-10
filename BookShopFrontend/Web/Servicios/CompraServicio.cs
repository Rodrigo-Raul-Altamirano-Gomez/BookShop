using Web.Modelos;
using Web.Modelos.DTO;

namespace Web.Servicios;

public class CompraServicio
{
    private readonly HttpClientUtils api;
    private readonly SesionUsuario Sesion;
    private readonly CatalogoServicio Catalogo;
    public Dictionary<int, int> Carrito { get; } = [];
    public List<Pedido> Pedidos { get; } = [];
    public ResumenCompraDTO? Resumen { get; private set; }
    public PreparacionCompraDTO? Preparacion { get; set; }
    public int CantidadCarrito => Resumen?.CantidadLibros ?? 0;

    public CompraServicio(HttpClientUtils api, SesionUsuario sesion, CatalogoServicio catalogo)
    {
        this.api = api;
        Sesion = sesion;
        Catalogo = catalogo;
        Sesion.Cerrada += Limpiar;
    }

    private void Limpiar()
    {
        Carrito.Clear();
        Pedidos.Clear();
        Resumen = null;
        Preparacion = null;
    }

    public void MostrarCarrito(ResumenCompraDTO resumen)
    {
        Resumen = resumen;
        Carrito.Clear();
        foreach (var dto in resumen.Items)
        {
            Carrito[dto.IdLibro] = dto.Cantidad;
            var libro = Catalogo.Libros.FirstOrDefault(libro => libro.Id == dto.IdLibro) ?? new Libro { Id = dto.IdLibro };
            libro.Titulo = dto.Titulo;
            libro.Portada = Presentacion.Imagen(dto.Portada, "/images/book-placeholder.svg");
            libro.PrecioOriginal = dto.PrecioOriginal;
            libro.Precio = dto.PrecioUnitario;
            libro.Descuento = dto.PorcentajeDescuento;
            libro.Stock = dto.StockDisponible ?? 0;
            libro.Activo = dto.Activo ?? true;
            Catalogo.Recordar(libro);
        }
        Sesion.Notificar();
    }

    public async Task AgregarLibro(int idLibro, int cantidad)
    {
        if (!Sesion.RequerirAcceso())
            return;
        MostrarCarrito(await api.Post<ResumenCompraDTO>($"carrito/libros/{idLibro}?cantidad={cantidad}"));
        Preparacion = null;
    }

    public async Task CambiarCantidad(int idLibro, int cantidad)
    {
        MostrarCarrito(await api.Put<ResumenCompraDTO>($"carrito/libros/{idLibro}?cantidad={cantidad}"));
        Preparacion = null;
    }

    public async Task VaciarCarrito()
    {
        MostrarCarrito(await api.Delete<ResumenCompraDTO>("carrito"));
        Preparacion = null;
    }
}
