using Web.Modelos;

namespace Web.Servicios;

// Adapta los contratos REST a los datos que muestran las pantallas existentes.
public static class Presentacion
{
    public static string Dinero(decimal valor) => "S/ " + valor.ToString("N2", System.Globalization.CultureInfo.GetCultureInfo("es-PE"));

    public static string Imagen(ImagenDTO? imagen, string defecto)
        => string.IsNullOrWhiteSpace(imagen?.Url) ? defecto : imagen.Url;

    public static ImagenDTO? ImagenDesdeUrl(string url)
        => string.IsNullOrWhiteSpace(url) ? null : new ImagenDTO(url.Trim());

    public static Libro Libro(TarjetaLibroDTO d) => new()
    {
        Id = d.Id,
        Titulo = d.Titulo,
        Autor = d.NombreAutor,
        PrecioOriginal = d.PrecioOriginal,
        Precio = d.PrecioConDescuento,
        Descuento = d.PorcentajeDescuento,
        Activo = d.Activo,
        Portada = Imagen(d.Portada, "/images/book-placeholder.svg")
    };

    public static Libro Libro(DetalleLibroDTO d)
    {
        var libro = Libro(d.Libro);
        libro.Categoria = d.NombreCategoria;
        libro.Editorial = d.NombreEditorial;
        libro.Idioma = d.Idioma;
        libro.Sinopsis = d.Sinopsis;
        libro.ISBN = d.Isbn;
        libro.Anio = d.AnioEdicion;
        libro.Paginas = d.NumeroPaginas;
        libro.Stock = d.StockDisponible;
        libro.Peso = d.Peso is null ? "—" : $"{d.Peso} kg";
        libro.Dimensiones = $"{d.Alto} × {d.Ancho} cm";
        libro.Saga = d.Saga ?? "";
        libro.Tomo = d.NumeroTomo;
        return libro;
    }

    public static Referencia Referencia(TarjetaReferenciaDTO d, string tipo) => new()
    {
        Id = d.Id,
        Nombre = d.Nombre,
        Tipo = tipo,
        Imagen = Imagen(d.Imagen, "/images/reference-placeholder.svg")
    };

    public static Direccion Direccion(DireccionEnvioDTO d) => new()
    {
        Id = d.Id,
        Alias = d.Alias,
        Calle = d.Calle,
        Distrito = d.Distrito,
        Ciudad = d.Ciudad,
        ReferenciaAdicional = d.Referencia ?? "",
        Predeterminada = d.Predeterminada
    };

    public static MetodoPago Metodo(MetodoPagoDTO d) => new()
    {
        Id = d.Id,
        Tipo = d.TarjetaBancaria ? "Tarjeta" : "Billetera",
        Titular = d.Titular,
        Alias = d.Alias ?? "",
        Proveedor = d.Proveedor,
        NumeroEnmascarado = d.NumeroEnmascarado,
        TipoTarjeta = d.TipoTarjeta == "CREDITO" ? "Crédito" : "Débito",
        Vencimiento = d.FechaVencimiento,
        Predeterminada = d.Predeterminado,
        Vigente = d.Vigente
    };

    public static Pedido Pedido(DetallePedidoDTO d) => new()
    {
        Id = d.Pedido.Id,
        FechaPago = d.Pedido.FechaPago,
        Estado = d.Pedido.Estado switch
        {
            "EN_ALMACEN" => "En almacén",
            "EN_CAMINO" => "En camino",
            "ENTREGADO" => "Entregado",
            "CANCELADO" => "Cancelado",
            "DEVUELTO" => "Devuelto",
            _ => d.Pedido.Estado
        },
        Direccion = Direccion(d.DireccionEnvio),
        Metodo = Metodo(d.MetodoPago),
        Productos = d.Resumen.SubtotalProductos,
        DescuentoLibro = d.Resumen.DescuentoLibros,
        Subtotal = d.Resumen.SubtotalProductos - d.Resumen.DescuentoLibros,
        DescuentoCupon = d.Resumen.DescuentoCupon,
        Envio = d.Resumen.CostoEnvio,
        Total = d.Resumen.Total,
        Transaccion = d.IdentificadorTransaccion,
        Seguimiento = d.CodigoSeguimiento,
        EntregaEstimada = d.FechaEntregaEstimada ?? default,
        FechaSalida = d.FechaSalida,
        FechaEntrega = d.FechaEntrega,
        Lineas = d.Resumen.Items.Select(i => new LineaPedido { IdLibro = i.IdLibro, Cantidad = i.Cantidad, Precio = i.PrecioUnitario, PrecioOriginal = i.PrecioOriginal, Descuento = i.PorcentajeDescuento }).ToList()
    };
}
