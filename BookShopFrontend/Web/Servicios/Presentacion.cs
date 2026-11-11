using Web.Modelos;

namespace Web.Servicios;

// Adapta los contratos REST a los datos que muestran las pantallas existentes.
public static class Presentacion
{
    public static string Dinero(decimal valor) =>
        "S/ " + valor.ToString("N2", System.Globalization.CultureInfo.GetCultureInfo("es-PE"));

    public static Libro Libro(TarjetaLibroDTO dto) => new()
    {
        Id = dto.Id,
        Titulo = dto.Titulo,
        Autor = dto.NombreAutor,
        PrecioOriginal = dto.PrecioOriginal,
        Precio = dto.PrecioConDescuento,
        Descuento = dto.PorcentajeDescuento,
        Activo = dto.Activo,
        Portada = dto.Portada ?? "/images/book-placeholder.svg"
    };

    public static Libro Libro(DetalleLibroDTO dto)
    {
        var libro = Libro(dto.Libro);
        libro.Categoria = dto.NombreCategoria;
        libro.Editorial = dto.NombreEditorial;
        libro.Idioma = dto.Idioma;
        libro.Sinopsis = dto.Sinopsis;
        libro.ISBN = dto.Isbn;
        libro.Anio = dto.AnioEdicion;
        libro.Paginas = dto.NumeroPaginas;
        libro.Stock = dto.StockDisponible;
        libro.Peso = dto.Peso is null ? "—" : $"{dto.Peso} kg";
        libro.Dimensiones = $"{dto.Alto} × {dto.Ancho} cm";
        libro.Saga = dto.Saga ?? "";
        libro.Tomo = dto.NumeroTomo;
        return libro;
    }

    public static Referencia Referencia(TarjetaReferenciaDTO dto, string tipo) => new()
    {
        Id = dto.Id,
        Nombre = dto.Nombre,
        Tipo = tipo,
        Imagen = dto.Imagen ?? "/images/reference-placeholder.svg"
    };

    public static Direccion Direccion(DireccionEnvioDTO dto) => new()
    {
        Id = dto.Id,
        Alias = dto.Alias,
        Calle = dto.Calle,
        Distrito = dto.Distrito,
        Ciudad = dto.Ciudad,
        ReferenciaAdicional = dto.Referencia ?? "",
        Predeterminada = dto.Predeterminada
    };

    public static MetodoPago Metodo(MetodoPagoDTO dto) => new()
    {
        Id = dto.Id,
        Tipo = dto.TarjetaBancaria ? "Tarjeta" : "Billetera",
        Titular = dto.Titular,
        Alias = dto.Alias ?? "",
        Proveedor = dto.Proveedor,
        NumeroEnmascarado = dto.NumeroEnmascarado,
        TipoTarjeta = dto.TipoTarjeta == "CREDITO" ? "Crédito" : "Débito",
        Vencimiento = dto.FechaVencimiento,
        Predeterminada = dto.Predeterminado,
        Vigente = dto.Vigente
    };

    public static Pedido Pedido(DetallePedidoDTO dto)
    {
        var lineas = new List<LineaPedido>();
        foreach (var item in dto.Resumen.Items)
        {
            lineas.Add(new LineaPedido
            {
                IdLibro = item.IdLibro,
                Titulo = item.Titulo,
                Portada = item.Portada,
                Cantidad = item.Cantidad,
                Precio = item.PrecioUnitario,
                PrecioOriginal = item.PrecioOriginal,
                Descuento = item.PorcentajeDescuento
            });
        }
        return new Pedido
        {
            Id = dto.Pedido.Id,
            FechaPago = dto.Pedido.FechaPago,
            Estado = dto.Pedido.Estado switch
            {
                "EN_ALMACEN" => "En almacén",
                "EN_CAMINO" => "En camino",
                "ENTREGADO" => "Entregado",
                "CANCELADO" => "Cancelado",
                "DEVUELTO" => "Devuelto",
                _ => dto.Pedido.Estado
            },
            Direccion = Direccion(dto.DireccionEnvio),
            Metodo = Metodo(dto.MetodoPago),
            Productos = dto.Resumen.SubtotalProductos,
            DescuentoLibro = dto.Resumen.DescuentoLibros,
            Subtotal = dto.Resumen.SubtotalProductos - dto.Resumen.DescuentoLibros,
            DescuentoCupon = dto.Resumen.DescuentoCupon,
            Envio = dto.Resumen.CostoEnvio,
            Total = dto.Resumen.Total,
            Transaccion = dto.IdentificadorTransaccion,
            Seguimiento = dto.CodigoSeguimiento,
            EntregaEstimada = dto.FechaEntregaEstimada ?? default,
            FechaSalida = dto.FechaSalida,
            FechaEntrega = dto.FechaEntrega,
            Lineas = lineas
        };
    }
}
