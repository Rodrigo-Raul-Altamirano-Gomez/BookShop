using Web.Modelos;

namespace Web.Demostracion;

public static class DatosDemostracion
{
    public static List<Libro> CrearLibros()
    {
        var titulos = new[]
        {
            "Dune",
            "Cien años de soledad",
            "1984",
            "El principito",
            "La biblioteca de medianoche",
            "El viento conoce mi nombre",
            "La sombra del viento",
            "Hábitos atómicos",
            "El alquimista",
            "Sapiens",
            "Lecciones de química",
            "Proyecto Hail Mary",
            "La vida imposible",
            "El problema de los tres cuerpos",
            "La paciente silenciosa",
            "Fundación"
        };
        var autores = new[]
        {
            "Frank Herbert",
            "Gabriel García Márquez",
            "George Orwell",
            "Antoine de Saint-Exupéry",
            "Matt Haig",
            "Isabel Allende",
            "Carlos Ruiz Zafón",
            "James Clear",
            "Paulo Coelho",
            "Yuval Noah Harari",
            "Bonnie Garmus",
            "Andy Weir",
            "Matt Haig",
            "Cixin Liu",
            "Alex Michaelides",
            "Isaac Asimov"
        };
        var categorias = new[]
        {
            "Ciencia ficción",
            "Literatura",
            "Literatura",
            "Literatura",
            "Literatura",
            "Literatura",
            "Literatura",
            "Desarrollo personal",
            "Literatura",
            "Historia",
            "Literatura",
            "Ciencia ficción",
            "Literatura",
            "Ciencia ficción",
            "Literatura",
            "Ciencia ficción"
        };
        var editoriales = new[]
        {
            "Debolsillo",
            "Penguin Random House",
            "Debolsillo",
            "Anagrama",
            "Planeta",
            "Penguin Random House",
            "Planeta",
            "Planeta",
            "Planeta",
            "Debolsillo",
            "Alfaguara",
            "Penguin Random House",
            "Planeta",
            "Debolsillo",
            "Alfaguara",
            "Debolsillo"
        };
        var precios = new decimal[]
        {
            59.90m,
            59.90m,
            49.90m,
            39.90m,
            54.90m,
            59.90m,
            55m,
            49.90m,
            59.90m,
            59.90m,
            54.90m,
            59.90m,
            54.90m,
            54.90m,
            49.90m,
            49.90m
        };
        var libros = new List<Libro>();
        for (var i = 0; i < titulos.Length; i++)
        {
            libros.Add(new Libro
            {
                Id = i + 1,
                Titulo = titulos[i],
                Autor = autores[i],
                Categoria = categorias[i],
                Editorial = editoriales[i],
                Precio = precios[i],
                PrecioOriginal = i is 0 or 2 or 11 ? 69.90m : precios[i],
                Descuento = i is 0 or 2 or 11 ? 15 : 0,
                Portada = $"/images/cover-{i + 1}.svg",
                Paginas = i == 0 ? 704 : 240 + i * 20,
                Anio = 2020 + i % 7,
                Stock = 8 + i,
                Idioma = i == 11 ? "Inglés" : "Español",
                ISBN = $"978840000{(i + 1000):D4}",
                Saga = i == 0 ? "Dune" : "",
                Tomo = i == 0 ? 1 : null,
                Sinopsis = i == 0 ? ("En el planeta desértico Arrakis, Paul Atreides se enfrenta a un "
                 + "destino que cambiará el equilibrio del universo. Su familia debe "
                     + "gobernar un territorio hostil donde la especia se convierte en el "
                        + "centro de las disputas por el poder. Entre intrigas políticas y "
                        + "antiguas tradiciones, Paul descubrirá los secretos de un mundo "
                        + "extraordinario y el papel que le corresponde en su futuro. Una "
                        + "historia de poder, supervivencia y esperanza.") : (
                        "Hay libros que se quedan contigo mucho después de la última página. "
                        + "Esta historia nos invita a mirar el mundo desde otra perspectiva, a "
                        + "descubrir personajes inolvidables y a explorar los pequeños "
                        + "detalles que dan sentido a la vida. Una lectura para detener el "
                        + "tiempo, despertar la curiosidad y encontrar nuevas preguntas.")
            });
        }

        libros[0].Descuentos.Add(new DescuentoLibro());
        return libros;
    }

    public static List<Referencia> CrearReferencias()
    {
        var resultado = new List<Referencia>();
        var grupos = new Dictionary<string, string[]>
        {

            ["categorias"] = ["Literatura", "Ciencia ficción", "Historia", "Arte", "Desarrollo personal",
                 "Filosofía"],

            ["autores"] = ["Frank Herbert", "Gabriel García Márquez", "Isabel Allende", "Matt Haig",
                 "George Orwell", "Antoine de Saint-Exupéry", "Carlos Ruiz Zafón", "James Clear",
                      "Paulo Coelho", "Yuval Noah Harari", "Bonnie Garmus", "Andy Weir", "Cixin Liu",
                         "Alex Michaelides", "Isaac Asimov"],

            ["editoriales"] = ["Penguin Random House", "Planeta", "Anagrama", "Alfaguara", "Debolsillo"]

        };
        foreach (var grupo in grupos)
            for (var i = 0; i < grupo.Value.Length; i++)
                resultado.Add(new Referencia
                {
                    Id = i + 1,
                    Tipo = grupo.Key,
                    Nombre = grupo.Value[i],
                    Imagen = $"/images/{grupo.Key}-{i % 6 + 1}.svg",
                    Descripcion = grupo.Key == "autores" ? ($"{grupo.Value[
             i]} nos invita a descubrir nuevas historias y formas de ver el mundo. "
                 + $"Explora su obra y encuentra tu próxima lectura entre estos libros.") : (
                     $"Un espacio para descubrir " + $"{grupo.Value[i].ToLowerInvariant(
                        )}, historias que inspiran y libros que acompañan. Explora nuestra "
                        + $"selección y encuentra una lectura para ti.")
                });
        return resultado;
    }

    public static List<Resena> CrearResenas() => [new()
    {

        Id = 1,

        IdLibro = 1,

        Usuario = "María López",

        Propia = true,

        Puntuacion = 5,

        Votos = 8,

        Comentario = ("Una historia fascinante, con un universo muy bien construido. Me "
            + "atrapó desde las primeras páginas.")

}, new()
    {

        Id = 2,

        IdLibro = 1,

        Usuario = "Carlos Ruiz",

        Puntuacion = 4,

        Votos = 3,

        Comentario = ("Una lectura intensa que merece la pena. Los personajes y la "
            + "ambientación son extraordinarios.")

}, new()
    {

        Id = 3,

        IdLibro = 2,

        Usuario = "María López",

        Propia = true,

        Puntuacion = 5,

        Votos = 12,

        Comentario = "Una novela a la que siempre quiero volver. Cada lectura descubre algo nuevo."

}, new()
    {

        Id = 4,

        IdLibro = 3,

        Usuario = "Ana Torres",

        Puntuacion = 3,

        Votos = 1,

        Comentario = "Una historia que invita a reflexionar y conversar después de leerla."

}, new()
    {

        Id = 5,

        IdLibro = 6,

        Usuario = "María López",

        Propia = true,

        Puntuacion = 4,

        Votos = 2,

        Comentario = "Personajes cercanos y una historia emotiva. Muy recomendable."

}

    ];
    public static List<Direccion> CrearDirecciones() => [new()
    {

        Id = 1,

        Alias = "Casa",

        Calle = "Av. Los Olivos 123",

        Distrito = "Miraflores",

        Ciudad = "Lima",

        ReferenciaAdicional = "Puerta verde",

        Predeterminada = true

}, new()
    {

        Id = 2,

        Alias = "Trabajo",

        Calle = "Calle Las Flores 456",

        Distrito = "San Isidro",

        Ciudad = "Lima"

}, new()
    {

        Id = 3,

        Alias = "Familia",

        Calle = "Av. Primavera 890",

        Distrito = "San Borja",

        Ciudad = "Lima",

        ReferenciaAdicional = "Departamento 302"

}

    ];
    public static List<MetodoPago> CrearMetodos() => [new()
    {

        Id = 1,

        Alias = "Tarjeta personal",

        Proveedor = "Visa",

        TipoTarjeta = "Crédito",

        Predeterminada = true

}, new()
    {

        Id = 2,

        Alias = "Billetera",

        Tipo = "Billetera",

        Proveedor = "Yape",

        NumeroEnmascarado = "••••• 0562"

}, new()
    {

        Id = 3,

        Alias = "Tarjeta de compras",

        Proveedor = "Mastercard",

        TipoTarjeta = "Débito",

        NumeroEnmascarado = "•••• •••• •••• 8150"

}

    ];
    public static List<Cupon> CrearCupones() => [new()
    {

        Id = 1

}, new()
    {

        Id = 2,

        Porcentaje = 15,

        Minimo = 200

}, new()
    {

        Id = 3,

        Estado = "Utilizado",

        Vencimiento = new(2026, 11, 4)

}, new()
    {

        Id = 4,

        Estado = "Vencido",

        Vencimiento = new(2026, 9, 1)

}

    ];
    public static List<Solicitud> CrearSolicitudes() => [new()
    {

        Id = 101,

        Tipo = "Devoluciones",

        IdReferencia = 43,

        Motivo = "Libro dañado",

        Comentario = "La portada llegó con una esquina doblada."

}, new()
    {

        Id = 102,

        Tipo = "Devoluciones",

        IdReferencia = 44,

        Motivo = "Libro incorrecto",

        Estado = "Aceptada",

        FechaResolucion = new(2026, 10, 6),

        Comentario = "Recibí una edición diferente."

}, new()
    {

        Id = 103,

        Tipo = "Reportes",

        IdReferencia = 4,

        Motivo = "Contenido ajeno al libro",

        Comentario = "El comentario no trata sobre la lectura.",

        ComentarioOriginal = "Una historia que invita a reflexionar y conversar después de leerla.",

        PuntuacionOriginal = 3

}, new()
    {

        Id = 104,

        Tipo = "Reportes",

        IdReferencia = 2,

        Motivo = "Otro",

        Estado = "Rechazada",

        FechaResolucion = new(2026, 10, 6),

        ComentarioOriginal = "Una lectura intensa que merece la pena.",

        PuntuacionOriginal = 4

}

    ];
    public static List<Pedido> CrearPedidos(Direccion direccion, MetodoPago metodo) => [PedidoEjemplo(42,
         "En almacén", direccion, metodo), PedidoEjemplo(43, "Entregado", direccion, metodo),
              PedidoEjemplo(44, "Devuelto", direccion, metodo), PedidoEjemplo(45, "En camino", direccion,
                   metodo), PedidoEjemplo(46, "Cancelado", direccion, metodo)];
    private static Pedido PedidoEjemplo(int id, string estado, Direccion direccion, MetodoPago metodo) => new()
    {

        Id = id,

        Estado = estado,

        Direccion = direccion,

        Metodo = metodo,

        FechaPago = new(2026, 10, id - 41, 10, 30, 0),

        Lineas = [new()
        {

            IdLibro = 1,

            Cantidad = 1,

            PrecioOriginal = 69.90m,

            Precio = 59.90m,

            Descuento = 15

    }, new()
        {

            IdLibro = 16,

            Cantidad = 2,

            Precio = 49.90m,

            PrecioOriginal = 49.90m

    }

        ],

        Productos = 169.70m,

        DescuentoLibro = 10m,

        Subtotal = 159.70m,

        DescuentoCupon = 0,

        Envio = 7.99m,

        Total = 167.69m,

        Transaccion = $"SIM-202610-00{id}",

        Seguimiento = $"BS-PE-00{id}",

        EntregaEstimada = new(2026, 10, id - 41, 12, 0, 0),

        FechaSalida = estado is "En camino" or "Entregado" ? new(2026, 10, id - 41, 11, 0, 0) : null,

        FechaEntrega = estado == "Entregado" ? new(2026, 10, id - 41, 12, 0, 0) : null

    };
}

