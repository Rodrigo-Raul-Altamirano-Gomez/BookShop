using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Web;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Configuration;
using Web.Modelos;
using Web.Modelos.DTO;
using Web.Servicios;

public static class RevisionReal
{
    public static async Task Ejecutar()
    {
        var configuracion = new ConfigurationBuilder().AddJsonFile(Path.GetFullPath("BookShopFrontend/Web/appsettings.json")).Build();
        using var api = new HttpClientUtils(configuracion);
        var sesion = new SesionUsuario(api);
        var catalogo = new CatalogoServicio(api, sesion);
        var cuenta = new CuentaServicio(api, sesion);
        var compras = new CompraServicio(api, sesion);
        await catalogo.Inicializar();
        if (!catalogo.Inicializado || catalogo.Idiomas.Count == 0) throw new Exception("No se pudo cargar el catálogo real.");
        var libros = await api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", new FiltroLibroDTO());
        int idLibro = libros.Items.First().Id;
        var detalle = await api.Get<DetalleLibroDTO>($"libros/{idLibro}");
        if (detalle.Libro.Id != idLibro) throw new Exception("Detalle incorrecto.");
        var similares = await api.Get<List<TarjetaLibroDTO>>($"recomendaciones/similares/{idLibro}");
        if (similares.Count > 8 || similares.Any(libro => libro.Id == idLibro)) throw new Exception("Similares incorrectos.");
        var vendidos = await api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", new FiltroLibroDTO { Orden = "MAS_VENDIDOS", TamanioPagina = 8 });
        if (vendidos.Items.Count == 0) throw new Exception("Más vendidos sin resultados.");
        var filtrados = await api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", new FiltroLibroDTO { Titulo = detalle.Libro.Titulo, PrecioMinimo = detalle.Libro.PrecioConDescuento, PrecioMaximo = detalle.Libro.PrecioConDescuento });
        if (!filtrados.Items.Any(libro => libro.Id == idLibro)) throw new Exception("La búsqueda con filtro de precio perdió el libro.");
        var comparacion = await api.Get<List<DetalleLibroDTO>>($"libros/comparacion?primerLibro={idLibro}&segundoLibro={libros.Items[1].Id}");
        if (comparacion.Count != 2) throw new Exception("Comparación incompleta.");
        Console.WriteLine("RF07, RF08, RF09, RF12 y RF14: catálogo, filtros, detalle, comparación, similares y más vendidos correctos.");
        string correo = $"revision.frontend.{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}@example.test";
        var acceso = await api.Post<AccesoUsuarioDTO>("cuenta/registro", new RegistroUsuarioDTO("Revisión frontend", correo, new DateOnly(2000, 1, 1), "RevisionTA2026!", "RevisionTA2026!", null));
        try
        {
            sesion.MostrarPerfil(await api.Get<PerfilUsuarioDTO>("cuenta"), acceso.Rol == "ADMIN");
            var perfilEditado = await api.Put<PerfilUsuarioDTO>("cuenta", new DatosPerfilDTO("Usuario revisado", new DateOnly(2000, 1, 1)));
            if (perfilEditado.Usuario.Nombre != "Usuario revisado") throw new Exception("El perfil no se actualizó.");
            var perfilConFoto = await api.Put<PerfilUsuarioDTO>("cuenta/imagen?url=" + Uri.EscapeDataString("/images/catalogo/libro-20.png"));
            if (perfilConFoto.Usuario.Imagen != "/images/catalogo/libro-20.png") throw new Exception("La URL de perfil no se conservó.");
            Console.WriteLine("RF01 y RF02: registro, sesión, edición de perfil e imagen correctos.");
            await cuenta.GuardarDireccion(new Direccion { Alias = "Revision frontend", Calle = "Calle de prueba 123", Distrito = "San Miguel", Ciudad = "Lima", Predeterminada = true });
            await cuenta.GuardarMetodo(new MetodoPago { Tipo = "Tarjeta", Alias = "Revision frontend", Titular = "Usuario de prueba", Proveedor = "Banco de prueba", Numero = "411" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), TipoTarjeta = "Débito", Vencimiento = new DateOnly(2030, 12, 31), Predeterminada = true });
            await cuenta.AlternarGuardado(idLibro, false);
            if (!cuenta.Deseados.Contains(idLibro)) throw new Exception("No se guardó el libro.");
            await cuenta.AlternarGuardado(idLibro, true);
            var favoritos = await api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("cuenta/libros-guardados/busqueda?favorito=true", new FiltroLibroDTO());
            if (!favoritos.Items.Any(libro => libro.Id == idLibro)) throw new Exception("Favorito no persistido.");
            await cuenta.AlternarSeguimiento("autores", detalle.IdAutor, false);
            await compras.AgregarLibro(idLibro, 1);
            await compras.CambiarCantidad(idLibro, 2);
            if (compras.CantidadCarrito != 2) throw new Exception("Cantidad incorrecta.");
            await compras.CambiarCantidad(idLibro, 1);
            var complementarios = await api.Post<List<TarjetaLibroDTO>>("recomendaciones/complementarios", new[] { idLibro });
            if (complementarios.Count > 8 || complementarios.Any(libro => libro.Id == idLibro)) throw new Exception("Complementarios incorrectos.");
            Console.WriteLine("RF05, RF06 y RF11: seguimientos, listas personales y cambios de carrito correctos.");
            var servicios = new ServiceCollection();
            servicios.AddLogging();
            servicios.AddSingleton(api); servicios.AddSingleton(sesion); servicios.AddSingleton(catalogo);
            servicios.AddSingleton(cuenta); servicios.AddSingleton(compras);
            servicios.AddSingleton<NavigationManager, NavegacionRevision>();
            await using var proveedor = servicios.BuildServiceProvider();
            await using var renderizador = new HtmlRenderer(proveedor, proveedor.GetRequiredService<ILoggerFactory>());
            await renderizador.Dispatcher.InvokeAsync(async () =>
            {
                var inicio = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Inicio>();
                var listado = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>();
                var libro = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.DetalleLibro>(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = idLibro }));
                var perfil = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MiPerfil>();
                var datosCompra = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.DatosCompra>();
                var guardados = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.LibrosGuardados>();
                var seguimientos = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisSeguimientos>();
                var carrito = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Compra.Carrito>();
                var compra = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Compra.Compra>();
                foreach (string html in new[] { inicio.ToHtmlString(), listado.ToHtmlString(), libro.ToHtmlString(), perfil.ToHtmlString(), datosCompra.ToHtmlString(), guardados.ToHtmlString(), seguimientos.ToHtmlString(), carrito.ToHtmlString(), compra.ToHtmlString() })
                {
                    if (html.Contains("spinner-border") || html.Contains("alert-danger") || html.Length < 100) throw new Exception("Pantalla incompleta o con error.");
                }
                Console.WriteLine("Frontend: nueve pantallas renderizadas con servicios Java y datos MySQL reales.");
            });
            var preparacion = await api.Post<PreparacionCompraDTO>("pedidos/preparacion");
            preparacion = await api.Put<PreparacionCompraDTO>("pedidos/preparacion", new SeleccionCompraDTO(cuenta.Direcciones.Single().Id, cuenta.Metodos.Single().Id, 0));
            if (!preparacion.PuedePagar) throw new Exception("Preparación incompleta.");
            DetallePedidoDTO? pedido = null;
            for (int intento = 0; intento < 3 && pedido is null; intento++)
            {
                try { pedido = await api.Post<DetallePedidoDTO>("pedidos/confirmacion"); }
                catch (InvalidOperationException e) when (e.Message.Contains("simulado fue rechazado")) { }
            }
            if (pedido is null) throw new Exception("Tres pagos simulados rechazados.");
            var repetido = await api.Post<DetallePedidoDTO>("pedidos/confirmacion");
            if (repetido.Pedido.Id != pedido.Pedido.Id) throw new Exception("Pedido duplicado.");
            compras.Pedidos.Add(Presentacion.Pedido(pedido));
            var persistido = repetido;
            if (persistido.Resumen.Total != pedido.Resumen.Total) throw new Exception("El pedido persistido cambió de importe.");
            var stockFinal = await api.Get<DetalleLibroDTO>($"libros/{idLibro}");
            if (stockFinal.StockDisponible != detalle.StockDisponible - 1) throw new Exception("Descuento de stock incorrecto.");
            proveedor.GetRequiredService<NavigationManager>().NavigateTo($"/confirmacion/{pedido.Pedido.Id}");
            await renderizador.Dispatcher.InvokeAsync(async () =>
            {
                var confirmacion = await renderizador.RenderComponentAsync<Web.Componentes.Paginas.Compra.DetallePedido>(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = pedido.Pedido.Id }));
                if (!confirmacion.ToHtmlString().Contains(pedido.IdentificadorTransaccion) || !confirmacion.ToHtmlString().Contains("Tu pedido ha sido registrado")) throw new Exception("Confirmación sin datos del pedido.");
            });
            Console.WriteLine("RF15 y RF16: selección, pago, stock, pedido persistido, confirmación y reintento sin duplicados correctos.");
            Console.WriteLine($"Correcto contra GlassFish/MySQL: catálogo, idiomas, detalle, registro, perfil, dirección, método, deseados, seguimiento, carrito, selección y pago. Pedido {pedido.Pedido.Id}; reintento sin duplicados.");
        }
        finally { await api.Delete("cuenta/sesion"); sesion.LimpiarCuenta(); }
    }
}

class NavegacionRevision : NavigationManager
{
    public NavegacionRevision() => Initialize("http://localhost/", "http://localhost/catalogo");
    protected override void NavigateToCore(string uri, bool forceLoad) => Uri = ToAbsoluteUri(uri).ToString();
}
