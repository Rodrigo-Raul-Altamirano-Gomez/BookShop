using Web.Modelos;

namespace Web.Servicios;

// Adapta los contratos REST a los datos que muestran las pantallas existentes.
public static class Presentacion
{
    public static string Dinero(decimal valor) =>
        "S/ " + valor.ToString("N2", System.Globalization.CultureInfo.GetCultureInfo("es-PE"));

    public static string EstadoPedido(string estado) => estado switch
    {
        "EN_ALMACEN" => "En almacén",
        "EN_CAMINO" => "En camino",
        "ENTREGADO" => "Entregado",
        "CANCELADO" => "Cancelado",
        "DEVUELTO" => "Devuelto",
        _ => estado
    };

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
        libro.IdAutor = dto.IdAutor;
        libro.IdCategoria = dto.IdCategoria;
        libro.IdEditorial = dto.IdEditorial;
        libro.Categoria = dto.NombreCategoria;
        libro.Editorial = dto.NombreEditorial;
        libro.Idioma = dto.Idioma;
        libro.Sinopsis = dto.Sinopsis;
        libro.ISBN = dto.Isbn;
        libro.Anio = dto.AnioEdicion;
        libro.Paginas = dto.NumeroPaginas;
        libro.Stock = dto.StockDisponible;
        libro.Peso = dto.Peso is null ? "—" : $"{dto.Peso} kg";
        libro.Dimensiones = dto.Alto is null || dto.Ancho is null ? "—" : $"{dto.Alto} × {dto.Ancho} cm";
        libro.Saga = dto.Saga ?? "";
        libro.Tomo = dto.NumeroTomo;
        libro.Valoracion = dto.Valoracion;
        return libro;
    }

    public static Referencia Referencia(TarjetaReferenciaDTO dto, string tipo) => new()
    {
        Id = dto.Id,
        Nombre = dto.Nombre,
        Tipo = tipo,
        Imagen = dto.Imagen ?? "/images/reference-placeholder.svg"
    };

    public static string TipoMetodo(MetodoPagoDTO metodo)
    {
        if (!metodo.TarjetaBancaria)
            return "Billetera";
        return metodo.TipoTarjeta == "CREDITO" ? "Crédito" : "Débito";
    }

    public static string NombreMetodo(MetodoPagoDTO metodo)
    {
        if (!string.IsNullOrWhiteSpace(metodo.Alias))
            return metodo.Alias;
        return metodo.TarjetaBancaria ? "Tarjeta" : "Billetera";
    }

    public static string EstadoCupon(string estado) => estado switch
    {
        "DISPONIBLE" => "Disponible",
        "VENCIDO" => "Vencido",
        _ => "Usado"
    };
}
