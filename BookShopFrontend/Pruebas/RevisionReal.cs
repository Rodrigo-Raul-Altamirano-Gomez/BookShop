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
        var cuenta = new CuentaServicio(api, sesion, catalogo);
        var compras = new CompraServicio(api, sesion, catalogo);
        await catalogo.Inicializar();
        if (!catalogo.Inicializado || catalogo.Idiomas.Count == 0) throw new Exception("No se pudo cargar el catálogo real.");
        var libros = await api.Post<ResultadoPaginadoDTO<TarjetaLibroDTO>>("libros/busqueda", CatalogoServicio.Filtro());
        int idLibro = libros.Items.First().Id;
        var detalle = await api.Get<DetalleLibroDTO>($"libros/{idLibro}");
        if (detalle.Libro.Id != idLibro) throw new Exception("Detalle incorrecto.");
        string correo = $"revision.frontend.{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}@example.test";
        var acceso = await api.Post<AccesoUsuarioDTO>("cuenta/registro", new RegistroUsuarioDTO("Revisión frontend", correo, new DateOnly(2000, 1, 1), "RevisionTA2026!", "RevisionTA2026!", null));
        try
        {
            sesion.MostrarPerfil(await api.Get<PerfilUsuarioDTO>("cuenta"), acceso.Rol == "ADMIN");
            await cuenta.GuardarDireccion(new Direccion { Alias = "Revision frontend", Calle = "Calle de prueba 123", Distrito = "San Miguel", Ciudad = "Lima", Predeterminada = true });
            await cuenta.GuardarMetodo(new MetodoPago { Tipo = "Tarjeta", Alias = "Revision frontend", Titular = "Usuario de prueba", Proveedor = "Banco de prueba", Numero = "411" + DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), TipoTarjeta = "Débito", Vencimiento = new DateOnly(2030, 12, 31), Predeterminada = true });
            await cuenta.AlternarGuardado(idLibro, false);
            if (!cuenta.Deseados.Contains(idLibro)) throw new Exception("No se guardó el libro.");
            await cuenta.AlternarSeguimiento("autores", detalle.IdAutor);
            await compras.AgregarLibro(idLibro, 1);
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
            Console.WriteLine($"Correcto contra GlassFish/MySQL: catálogo, idiomas, detalle, registro, perfil, dirección, método, deseados, seguimiento, carrito, selección y pago. Pedido {pedido.Pedido.Id}; reintento sin duplicados.");
        }
        finally { await api.Delete("cuenta/sesion"); sesion.LimpiarCuenta(); }
    }
}
