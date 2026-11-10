using Web.Modelos.DTO;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Web;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using Web.Servicios;

if (args.Contains("--real")) { await RevisionReal.Ejecutar(); return; }

// Servidor de prueba: verifica el contrato HTTP del cliente, sin sustituir una prueba con Java/MySQL.
var builder = WebApplication.CreateBuilder();
builder.WebHost.UseUrls("http://127.0.0.1:0");
builder.Logging.ClearProviders();
await using var servidor = builder.Build();
int verificaciones = 0;
string? ultimoOrden = null;
DatosMetodoPagoDTO? ultimoMetodo = null;
Task esperaBusqueda = Task.CompletedTask;
bool fallarBusqueda = false;
void Verificar(bool condicion, string mensaje)
{
    if (!condicion) throw new Exception(mensaje);
    verificaciones++;
}
var resumen = new ResumenCompraDTO([new ItemDTO(7, "Libro real", null, 2, 50, 40, 20, 80, true, 10)], 100, 20, 0, 0, 0, 5, 4, 84, 2);
var perfil = new PerfilUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana", "ana@correo.pe", null), new DateOnly(2000, 1, 2));
var seleccion = new SeleccionCompraDTO(3, 4, 0);
servidor.Map("/{**ruta}", async contexto =>
{
    var ruta = contexto.Request.Path.Value!;
    if (contexto.Request.Method != "GET") Verificar(contexto.Request.Headers["X-BookShop-Cliente"] == "web", "Cabecera REST ausente");
    object? resultado = null;
    if (ruta == "/webresources/cuenta/sesion")
    {
        var entrada = await contexto.Request.ReadFromJsonAsync<CredencialesDTO>();
        Verificar(entrada?.Email == "ana@correo.pe" && entrada.Contrasenia == "clave123", "Credenciales JSON incorrectas");
        contexto.Response.Cookies.Append("JSESSIONID", "sesion-ana", new CookieOptions { Path = "/webresources", HttpOnly = true });
        resultado = new AccesoUsuarioDTO(1, "Ana", "CLIENTE", false);
    }
    else if (ruta == "/webresources/cookie") resultado = contexto.Request.Cookies["JSESSIONID"] ?? "sin-sesion";
    else if (ruta == "/webresources/error")
    {
        contexto.Response.StatusCode = 401;
        await contexto.Response.WriteAsJsonAsync(new { codigo = "SIN_SESION", mensaje = "Inicia sesión." }); return;
    }
    else if (ruta == "/webresources/cuenta/imagen")
    {
        using var cuerpo = await JsonDocument.ParseAsync(contexto.Request.Body);
        Verificar(cuerpo.RootElement.ValueKind == JsonValueKind.Null, "Retirar foto exige JSON null"); resultado = perfil;
    }
    else if (ruta == "/webresources/imagen")
    {
        using var cuerpo = await JsonDocument.ParseAsync(contexto.Request.Body);
        Verificar(cuerpo.RootElement.GetString() == "https://example.test/imagen.png", "URL de imagen incorrecta");
        resultado = "https://example.test/imagen.png";
    }
    else if (ruta == "/webresources/cuenta") resultado = perfil;
    else if (ruta == "/webresources/cuenta/direcciones") resultado = new ResultadoPaginadoDTO<DireccionEnvioDTO>([new(3, "Casa", "Calle 1", "Centro", "Lima", "", true)], 1, 1, 10);
    else if (ruta == "/webresources/cuenta/metodos")
    {
        if (contexto.Request.Method == "POST")
            ultimoMetodo = await contexto.Request.ReadFromJsonAsync<DatosMetodoPagoDTO>();
        resultado = new ResultadoPaginadoDTO<MetodoPagoDTO>([new(4, true, "Ana", "Banco", "Tarjeta", "****1234", "DEBITO", new DateOnly(2030, 1, 1), true, true)], 1, 1, 10);
    }
    else if (ruta == "/webresources/cuenta/cupones") resultado = new ResultadoPaginadoDTO<CuponDTO>([], 0, 1, 10);
    else if (ruta == "/webresources/cuenta/libros-guardados/busqueda")
    {
        Verificar(contexto.Request.Query.ContainsKey("favorito"), "Falta elegir la lista personal");
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>([], 0, 1, 12);
    }
    else if (ruta == "/webresources/cuenta/seguimientos/busqueda") resultado = new ResultadoPaginadoDTO<TarjetaReferenciaDTO>([], 0, 1, 12);
    else if (ruta == "/webresources/carrito" || ruta == "/webresources/carrito/libros/7") resultado = resumen;
    else if (ruta == "/webresources/pedidos/preparacion")
    {
        if (contexto.Request.Method == "POST") Verificar(contexto.Request.ContentType is null, "Preparación debe aceptar POST sin Content-Type");
        if (contexto.Request.Method == "PUT") seleccion = (await contexto.Request.ReadFromJsonAsync<SeleccionCompraDTO>())!;
        resultado = new PreparacionCompraDTO(resumen, seleccion, true);
    }
    else if (ruta == "/webresources/pedidos/confirmacion")
        resultado = new DetallePedidoDTO(new(21, new DateTime(2026, 11, 9, 10, 0, 0), "EN_ALMACEN", 2, 84), resumen,
            new(3, "Casa", "Calle 1", "Centro", "Lima", "", true),
            new(4, true, "Ana", "Banco", "Tarjeta", "****1234", "DEBITO", new DateOnly(2030, 1, 1), true, true), "SIM-21", "BS-21", null, null, null, false, false);
    else if (ruta.EndsWith("/autores/busqueda") || ruta.EndsWith("/categorias/busqueda") || ruta.EndsWith("/editoriales/busqueda"))
        resultado = new ResultadoPaginadoDTO<TarjetaReferenciaDTO>([new(1, "Referencia real", null)], 1, 1, 12);
    else if (ruta == "/webresources/libros/idiomas")
    {
        int pagina = int.Parse(contexto.Request.Query["pagina"]!);
        resultado = new ResultadoPaginadoDTO<string>(pagina == 1 ? Enumerable.Range(1, 12).Select(n => $"Idioma {n}").ToList() : ["Español"], 13, pagina, 12);
    }
    else if (ruta == "/webresources/libros/busqueda")
    {
        await esperaBusqueda;
        if (fallarBusqueda)
        {
            contexto.Response.StatusCode = 500;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "Fallo de consulta de prueba" });
            return;
        }
        var filtro = await contexto.Request.ReadFromJsonAsync<FiltroLibroDTO>(); ultimoOrden = filtro!.Orden;
        Verificar(filtro.SoloActivos && filtro.Pagina > 0 && filtro.TamanioPagina > 0, "El catálogo público debe enviar solo activos y paginación válida");
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>([new(7, "Libro real", null, "Autor real", 50, 40, 20, true)], 1, filtro.Pagina, filtro.TamanioPagina);
    }
    else if (ruta == "/webresources/libros/7")
        resultado = new DetalleLibroDTO(new(7, "Libro real", null, "Autor real", 50, 40, 20, true), 1, 1, "Categoría real", 1, "Editorial real", "Español", "Sinopsis real", "1234567890123", 2026, 300, null, null, null, "", null, 10, new(7, 0, 0, 0, 0, 1), false, false);
    else if (ruta == "/webresources/recomendaciones/similares/7") resultado = Array.Empty<TarjetaLibroDTO>();
    else if (ruta == "/webresources/resenas/libro/7/busqueda")
        resultado = new ResultadoPaginadoDTO<ResenaDTO>([new(1, 7, "Libro real", null, "Lector real", null, 5, "Reseña real", new DateTime(2026, 11, 9), 2, false, true)], 1, 1, 10);
    else { contexto.Response.StatusCode = 204; return; }
    await contexto.Response.WriteAsJsonAsync(resultado);
});
await servidor.StartAsync();
var configuracion = new ConfigurationBuilder().AddInMemoryCollection(new Dictionary<string, string?> { ["Backend:Url"] = servidor.Urls.Single() + "/webresources" }).Build();
using var api = new HttpClientUtils(configuracion);
using var otroUsuario = new HttpClientUtils(configuracion);
var acceso = await api.Post<AccesoUsuarioDTO>("cuenta/sesion", new CredencialesDTO("ana@correo.pe", "clave123"));
Verificar(acceso.Id == 1, "Respuesta de acceso incorrecta");
Verificar(await api.Get<string>("cookie") == "sesion-ana", "Cookie no conservada");
Verificar(await otroUsuario.Get<string>("cookie") == "sin-sesion", "Cookies compartidas entre usuarios");
var estado = new SesionUsuario(api);
var catalogoServicio = new CatalogoServicio(api, estado);
var cuentaServicio = new CuentaServicio(api, estado, catalogoServicio);
var compraServicio = new CompraServicio(api, estado, catalogoServicio);
estado.MostrarPerfil(await api.Get<PerfilUsuarioDTO>("cuenta"));
await cuentaServicio.CargarDatosCompra();
compraServicio.MostrarCarrito(await api.Get<ResumenCompraDTO>("carrito"));
Verificar(estado.Usuario?.Nacimiento == new DateOnly(2000, 1, 2), "Fecha de perfil incorrecta");
Verificar(cuentaServicio.Direcciones.Single().Id == 3 && cuentaServicio.Metodos.Single().NumeroEnmascarado == "****1234", "Datos de compra incorrectos");
Verificar(compraServicio.Resumen?.Total == 84 && compraServicio.CantidadCarrito == 2, "Se alteraron los importes del backend");
await compraServicio.CambiarCantidad(7, 2);
var preparado = await api.Put<PreparacionCompraDTO>("pedidos/preparacion", new SeleccionCompraDTO(3, 4, 0));
Verificar(preparado.PuedePagar && preparado.Seleccion.IdDireccionEnvio == 3, "Selección de compra incorrecta");
var pedido = Presentacion.Pedido(await api.Post<DetallePedidoDTO>("pedidos/confirmacion"));
Verificar(pedido.Id == 21 && pedido.Total == 84 && pedido.Transaccion == "SIM-21" && pedido.FechaEntrega is null, "Confirmación alterada");
await api.Put<PerfilUsuarioDTO>("cuenta/imagen");
var imagen = await api.Post<string>("imagen", "https://example.test/imagen.png");
Verificar(Presentacion.ImagenDesdeUrl(Presentacion.Imagen(imagen, ""))! == imagen, "Imagen no reversible");
await api.Delete("sin-contenido");
try { await api.Get<object>("error"); throw new Exception("No se informó el error HTTP"); }
catch (InvalidOperationException e) { Verificar(e.Message == "Inicia sesión.", "Mensaje del backend perdido"); }
Verificar(estado.Usuario is null && compraServicio.Carrito.Count == 0 && cuentaServicio.Direcciones.Count == 0, "Sesión vencida conserva datos privados");
Verificar(await api.Get<string>("cookie") == "sin-sesion", "Cookie vencida no retirada");
var tarjeta = new Web.Modelos.MetodoPago { Tipo = "Tarjeta", TipoTarjeta = "Crédito", Numero = "4111111111111111", Vencimiento = new DateOnly(2030, 1, 1) };
await cuentaServicio.GuardarMetodo(tarjeta);
Verificar(ultimoMetodo?.TipoTarjeta == "CREDITO" && ultimoMetodo.FechaVencimiento == tarjeta.Vencimiento, "Se perdieron el tipo o vencimiento de la tarjeta");
Verificar(tarjeta.Numero is null, "Se conserva el número completo tras guardar");
var billetera = new Web.Modelos.MetodoPago { Tipo = "Billetera", Numero = "999999999" };
await cuentaServicio.GuardarMetodo(billetera);
Verificar(ultimoMetodo?.TarjetaBancaria == false && ultimoMetodo.TipoTarjeta is null && ultimoMetodo.FechaVencimiento is null, "Billetera enviada como tarjeta");

// Renderizar componentes reales con respuestas REST, sin navegador ni datos de demostración.
var servicios = new ServiceCollection();
servicios.AddLogging(); servicios.AddSingleton(estado);
servicios.AddSingleton(api); servicios.AddSingleton(catalogoServicio); servicios.AddSingleton(cuentaServicio); servicios.AddSingleton(compraServicio); servicios.AddSingleton<NavigationManager, NavegacionPrueba>();
await using var proveedor = servicios.BuildServiceProvider();
await using var renderer = new HtmlRenderer(proveedor, proveedor.GetRequiredService<ILoggerFactory>());
await renderer.Dispatcher.InvokeAsync(async () =>
{
    var inicio = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Inicio>();
    Verificar(inicio.ToHtmlString().Contains("Libro real"), "Inicio no muestra datos REST");
    Verificar(catalogoServicio.Idiomas.Count == 13 && catalogoServicio.Idiomas.Contains("Español"), "No se cargaron todas las páginas de idiomas");
    var catalogo = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Busqueda"] = "historia", ["OrdenInicial"] = "MAS_VENDIDOS" }));
    Verificar(catalogo.ToHtmlString().Contains("Libro real") && ultimoOrden == "MAS_VENDIDOS", "Catálogo no envía el orden al servidor");
    var bloqueo = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    esperaBusqueda = bloqueo.Task;
    var pendiente = renderer.BeginRenderingComponent<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>();
    var duranteCarga = pendiente.ToHtmlString();
    Verificar(duranteCarga.Contains("spinner-border") && !duranteCarga.Contains("empty-state") && !duranteCarga.Contains("Libro real"), "Durante la consulta se muestra contenido provisional");
    bloqueo.SetResult();
    await pendiente.QuiescenceTask;
    Verificar(pendiente.ToHtmlString().Contains("Libro real") && !pendiente.ToHtmlString().Contains("spinner-border"), "La carga no termina al recibir los datos");
    fallarBusqueda = true;
    var fallido = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>();
    Verificar(fallido.ToHtmlString().Contains("Fallo de consulta de prueba") && !fallido.ToHtmlString().Contains("empty-state") && !fallido.ToHtmlString().Contains("spinner-border"), "El error se muestra como una lista vacía o deja la carga activa");
    fallarBusqueda = false;
    var detalle = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.DetalleLibro>(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 7 }));
    var html = System.Net.WebUtility.HtmlDecode(detalle.ToHtmlString());
    Verificar(html.Contains("Sinopsis real") && html.Contains("Reseña real") && html.Contains("/referencias/categorias/1"), "Detalle no representa la respuesta REST");
});
await servidor.StopAsync();
Console.WriteLine($"Correcto: {verificaciones} comprobaciones de contrato HTTP, JSON, cookies y compra.");

class NavegacionPrueba : NavigationManager
{
    public NavegacionPrueba() => Initialize("http://localhost/", "http://localhost/catalogo");
    protected override void NavigateToCore(string uri, bool forceLoad) { }
}
