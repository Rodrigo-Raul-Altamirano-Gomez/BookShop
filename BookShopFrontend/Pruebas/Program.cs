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
FiltroLibroDTO? ultimoFiltro = null;
string? ultimaRutaLibros = null;
DatosMetodoPagoDTO? ultimoMetodo = null;
FiltroReferenciaDTO? ultimoFiltroReferencia = null;
string? ultimaRutaReferencia = null;
ConsultaSeguimientosDTO? ultimaConsultaSeguimientos = null;
Task esperaBusqueda = Task.CompletedTask;
bool fallarBusqueda = false;
bool fallarSeleccion = false;
bool fallarFoto = false;
bool fallarReferencias = false;
bool rechazarPago = false;
bool deseado = false, favorito = false;
int escriturasGuardados = 0;
void Verificar(bool condicion, string mensaje)
{
    if (!condicion) throw new Exception(mensaje);
    verificaciones++;
}
var resumen = new ResumenCompraDTO([new ItemDTO(7, "Libro real", null, 2, 50, 40, 20, 80, true, 10)], 100, 20, 0, 0, 0, 5, 4, 84, 2);
ResumenCompraDTO resumenPreparacion = resumen;
var perfil = new PerfilUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana", "ana@correo.pe", null), new DateOnly(2000, 1, 2));
var seleccion = new SeleccionCompraDTO(3, 4, 0);
servidor.Map("/{**ruta}", async contexto =>
{
    var ruta = contexto.Request.Path.Value!;
    object? resultado = null;
    if (ruta == "/webresources/cuenta/sesion")
    {
        var entrada = await contexto.Request.ReadFromJsonAsync<CredencialesDTO>();
        Verificar(entrada?.Email == "ana@correo.pe" && entrada.Contrasenia == "clave123", "Credenciales JSON incorrectas");
        contexto.Response.Cookies.Append("JSESSIONID", "sesion-ana", new CookieOptions { Path = "/webresources", HttpOnly = true });
        resultado = new AccesoUsuarioDTO(1, "Ana", "CLIENTE", false);
    }
    else if (ruta == "/webresources/cookie") resultado = contexto.Request.Cookies["JSESSIONID"] ?? "sin-sesion";
    else if (ruta == "/webresources/clave-incorrecta") {
        contexto.Response.StatusCode = 401;
        await contexto.Response.WriteAsJsonAsync(new { codigo = "CredencialesInvalidas", mensaje = "Contraseña incorrecta." }); return;
    }
    else if (ruta == "/webresources/error")
    {
        contexto.Response.StatusCode = 401;
        await contexto.Response.WriteAsJsonAsync(new { codigo = "SIN_SESION", mensaje = "Inicia sesión." }); return;
    }
    else if (ruta == "/webresources/cuenta/imagen")
    {
        using var cuerpo = await JsonDocument.ParseAsync(contexto.Request.Body);
        Verificar(cuerpo.RootElement.ValueKind == JsonValueKind.Null, "La foto se actualiza con JSON null y la URL en la consulta");
        if (fallarFoto)
        {
            contexto.Response.StatusCode = 400;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "No se pudo guardar la foto." });
            return;
        }
        resultado = new PerfilUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana", "ana@correo.pe",
            contexto.Request.Query["url"].FirstOrDefault()), perfil.FechaNacimiento);
    }
    else if (ruta == "/webresources/imagen")
    {
        using var cuerpo = await JsonDocument.ParseAsync(contexto.Request.Body);
        Verificar(cuerpo.RootElement.GetString() == "https://example.test/imagen.png", "URL de imagen incorrecta");
        resultado = "https://example.test/imagen.png";
    }
    else if (ruta == "/webresources/cuenta") resultado = perfil;
    else if (ruta == "/webresources/cuenta/direcciones") {
        var direccion = new DireccionEnvioDTO(3, "Casa", "Calle 1", "Centro", "Lima", "", true);
        resultado = contexto.Request.Method == "POST" ? direccion : new ResultadoPaginadoDTO<DireccionEnvioDTO>([direccion], 1, 1, 10);
    }
    else if (ruta == "/webresources/cuenta/metodos")
    {
        if (contexto.Request.Method == "POST")
            ultimoMetodo = await contexto.Request.ReadFromJsonAsync<DatosMetodoPagoDTO>();
        var metodo = new MetodoPagoDTO(4, true, "Ana", "Banco", "Tarjeta", "****1234", "DEBITO", new DateOnly(2030, 1, 1), true, true);
        resultado = contexto.Request.Method == "POST" ? metodo : new ResultadoPaginadoDTO<MetodoPagoDTO>([metodo], 1, 1, 10);
    }
    else if (ruta == "/webresources/cuenta/cupones") resultado = new ResultadoPaginadoDTO<CuponDTO>([], 0, 1, 10);
    else if (ruta == "/webresources/cuenta/libros-guardados/busqueda")
    {
        ultimoFiltro = await contexto.Request.ReadFromJsonAsync<FiltroLibroDTO>();
        ultimaRutaLibros = ruta + contexto.Request.QueryString;
        Verificar(contexto.Request.Query.ContainsKey("favorito"), "Falta elegir la lista personal");
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>([], 0, 1, 12);
    }
    else if (ruta == "/webresources/cuenta/libros-guardados/7")
    {
        escriturasGuardados++;
        bool guardar = contexto.Request.Method == "PUT";
        if (contexto.Request.Query["favorito"] == "true")
            favorito = guardar;
        else
            deseado = guardar;
        contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/cuenta/seguimientos/busqueda")
    {
        ultimaConsultaSeguimientos = await contexto.Request.ReadFromJsonAsync<ConsultaSeguimientosDTO>();
        resultado = new ResultadoPaginadoDTO<TarjetaReferenciaDTO>([], 0, ultimaConsultaSeguimientos!.Pagina, 12);
    }
    else if (ruta == "/webresources/carrito" || ruta == "/webresources/carrito/libros/7") resultado = resumen;
    else if (ruta == "/webresources/pedidos/preparacion")
    {
        if (contexto.Request.Method == "POST") Verificar(contexto.Request.ContentType is null, "Preparación debe aceptar POST sin Content-Type");
        if (contexto.Request.Method == "PUT")
        {
            if (fallarSeleccion)
            {
                contexto.Response.StatusCode = 400;
                await contexto.Response.WriteAsJsonAsync(new { mensaje = "Selección rechazada." });
                return;
            }
            seleccion = (await contexto.Request.ReadFromJsonAsync<SeleccionCompraDTO>())!;
        }
        resultado = new PreparacionCompraDTO(resumenPreparacion, seleccion, true);
    }
    else if (ruta == "/webresources/pedidos/confirmacion" && rechazarPago)
    {
        resumenPreparacion = new ResumenCompraDTO(resumen.Items, 110, 20, 0, 0, 0, 5, 4.5m, 94.5m, 2);
        contexto.Response.StatusCode = 409;
        await contexto.Response.WriteAsJsonAsync(new { mensaje = "Revisa los importes actualizados antes de confirmar." });
        return;
    }
    else if (ruta == "/webresources/pedidos/confirmacion" || ruta == "/webresources/pedidos/21")
        resultado = new DetallePedidoDTO(new(21, new DateTime(2026, 11, 9, 10, 0, 0), "EN_ALMACEN", 2, 84), resumen,
            new(3, "Casa", "Calle 1", "Centro", "Lima", "", true),
            new(4, true, "Ana", "Banco", "Tarjeta", "****1234", "DEBITO", new DateOnly(2030, 1, 1), true, true), "SIM-21", "BS-21", null, null, null, false, false);
    else if (ruta.EndsWith("/autores/busqueda") || ruta.EndsWith("/categorias/busqueda") || ruta.EndsWith("/editoriales/busqueda"))
    {
        if (fallarReferencias)
        {
            contexto.Response.StatusCode = 500;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "No se pudieron cargar las referencias." });
            return;
        }
        ultimoFiltroReferencia = await contexto.Request.ReadFromJsonAsync<FiltroReferenciaDTO>();
        ultimaRutaReferencia = ruta;
        bool sinCoincidencias = ultimoFiltroReferencia!.Nombre == "inexistente";
        resultado = new ResultadoPaginadoDTO<TarjetaReferenciaDTO>(
            sinCoincidencias ? [] : [new(1, "Referencia real", null)],
            sinCoincidencias ? 0 : (ultimoFiltroReferencia.Nombre.Length == 0 ? 1 : 25), ultimoFiltroReferencia.Pagina, 12);
    }
    else if (ruta == "/webresources/libros/idiomas")
    {
        int pagina = int.Parse(contexto.Request.Query["pagina"]!);
        resultado = new ResultadoPaginadoDTO<string>(pagina == 1 ? Enumerable.Range(1, 12).Select(n => $"Idioma {n}").ToList() : ["Español"], 13, pagina, 12);
    }
    else if (ruta.StartsWith("/webresources/libros/por-"))
    {
        ultimoFiltro = await contexto.Request.ReadFromJsonAsync<FiltroLibroDTO>();
        ultimaRutaLibros = ruta;
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>([], 0, ultimoFiltro!.Pagina, 12);
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
        ultimoFiltro = filtro;
        ultimaRutaLibros = ruta;
        Verificar(filtro.SoloActivos && filtro.Pagina > 0 && filtro.TamanioPagina > 0, "El catálogo público debe enviar solo activos y paginación válida");
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>([new(7, "Libro real", null, "Autor real", 50, 40, 20, true)], 1, filtro.Pagina, filtro.TamanioPagina);
    }
    else if (ruta == "/webresources/libros/comparacion")
        resultado = new List<DetalleLibroDTO>
        {
            new()
            {
                Libro = new(7, "Libro real", null, "Autor real", 50, 40, 20, true),
                Valoracion = new(7, 1, 0, 0, 0, 1),
                Idioma = "Español",
                StockDisponible = 10
            },
            new()
            {
                Libro = new(8, "Segundo libro", null, "Otro autor", 30, 30, 0, true),
                Valoracion = new(8, 0, 0, 0, 0, 0),
                Idioma = "Inglés",
                StockDisponible = 5
            }
        };
    else if (ruta == "/webresources/libros/7")
        resultado = new DetalleLibroDTO(new(7, "Libro real", null, "Autor real", 50, 40, 20, true), 1, 1, "Categoría real", 1, "Editorial real", "Español", "Sinopsis real", "1234567890123", 2026, 300, null, null, null, "", null, 10, new(7, 0, 0, 0, 0, 1), favorito, deseado);
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
var cuentaServicio = new CuentaServicio(api, estado);
var compraServicio = new CompraServicio(api, estado);
estado.MostrarPerfil(await api.Get<PerfilUsuarioDTO>("cuenta"));
await cuentaServicio.CargarDatosCompra();
compraServicio.MostrarCarrito(await api.Get<ResumenCompraDTO>("carrito"));
Verificar(estado.Usuario?.Nacimiento == new DateOnly(2000, 1, 2), "Fecha de perfil incorrecta");
Verificar(cuentaServicio.Direcciones.Single().Id == 3 && cuentaServicio.Metodos.Single().NumeroEnmascarado == "****1234", "Datos de compra incorrectos");
Verificar(compraServicio.Resumen?.Total == 84 && compraServicio.CantidadCarrito == 2, "Se alteraron los importes del backend");
await compraServicio.CambiarCantidad(7, 2);
var preparado = await api.Put<PreparacionCompraDTO>("pedidos/preparacion", new SeleccionCompraDTO(3, 4, 0));
Verificar(preparado.PuedePagar && preparado.Seleccion.IdDireccionEnvio == 3, "Selección de compra incorrecta");
var pedido = await api.Post<DetallePedidoDTO>("pedidos/confirmacion");
Verificar(pedido.Pedido.Id == 21 && pedido.Resumen.Total == 84 && pedido.IdentificadorTransaccion == "SIM-21" && pedido.FechaEntrega is null, "Confirmación alterada");
await api.Put<PerfilUsuarioDTO>("cuenta/imagen");
var imagen = await api.Post<string>("imagen", "https://example.test/imagen.png");
Verificar(imagen == "https://example.test/imagen.png", "URL JSON alterada");
await api.Delete("sin-contenido");
try { await api.Get<object>("clave-incorrecta"); }
catch (InvalidOperationException) { }
Verificar(estado.Usuario is not null && await api.Get<string>("cookie") == "sesion-ana", "Una contraseña incorrecta cerró la sesión");
try { await api.Get<object>("error"); throw new Exception("No se informó el error HTTP"); }
catch (InvalidOperationException e) { Verificar(e.Message == "Inicia sesión.", "Mensaje del backend perdido"); }
Verificar(estado.Usuario is null && compraServicio.CantidadCarrito == 0 && cuentaServicio.Direcciones.Count == 0, "Sesión vencida conserva datos privados");
Verificar(await api.Get<string>("cookie") == "sin-sesion", "Cookie vencida no retirada");
Verificar(await cuentaServicio.GuardarDireccion(new Web.Modelos.Direccion()) == 3, "No se devuelve la dirección nueva para seleccionarla en compra");
var tarjeta = new Web.Modelos.MetodoPago { Tipo = "Tarjeta", TipoTarjeta = "Crédito", Numero = "4111111111111111", Vencimiento = new DateOnly(2030, 1, 1) };
Verificar(await cuentaServicio.GuardarMetodo(tarjeta) == 4, "No se devuelve el método nuevo para seleccionarlo en compra");
Verificar(ultimoMetodo?.TipoTarjeta == "CREDITO" && ultimoMetodo.FechaVencimiento == tarjeta.Vencimiento, "Se perdieron el tipo o vencimiento de la tarjeta");
Verificar(cuentaServicio.Metodos.Single().Numero is null
    && cuentaServicio.Metodos.Single().NumeroEnmascarado == "****1234", "La lista de métodos muestra el número completo");
var billetera = new Web.Modelos.MetodoPago { Tipo = "Billetera", Numero = "999999999" };
await cuentaServicio.GuardarMetodo(billetera);
Verificar(ultimoMetodo?.TarjetaBancaria == false && ultimoMetodo.TipoTarjeta is null && ultimoMetodo.FechaVencimiento is null, "Billetera enviada como tarjeta");

var catalogoFallido = new CatalogoServicio(api, estado);
fallarReferencias = true;
await catalogoFallido.Inicializar();
Verificar(!catalogoFallido.Inicializado && catalogoFallido.Error == "No se pudieron cargar las referencias.", "Se oculta el error al cargar el catálogo");
estado.LimpiarCuenta();
Verificar(catalogoFallido.Error is not null, "Un cambio de sesión borra el error del catálogo pendiente");
fallarReferencias = false;
await catalogoFallido.Inicializar();
Verificar(catalogoFallido.Inicializado && catalogoFallido.Error is null, "Reintentar no recupera el catálogo ni limpia su error");

// Renderizar componentes reales con respuestas REST, sin navegador ni datos de demostración.
var servicios = new ServiceCollection();
var componentes = new ComponentesPrueba();
servicios.AddSingleton<IComponentActivator>(componentes);
servicios.AddLogging(); servicios.AddSingleton(estado);
servicios.AddSingleton(api); servicios.AddSingleton(catalogoServicio); servicios.AddSingleton(cuentaServicio); servicios.AddSingleton(compraServicio); servicios.AddSingleton<NavigationManager, NavegacionPrueba>();
await using var proveedor = servicios.BuildServiceProvider();
await using var renderer = new HtmlRenderer(proveedor, proveedor.GetRequiredService<ILoggerFactory>());
await renderer.Dispatcher.InvokeAsync(async () =>
{
    estado.MostrarPerfil(perfil);
    var compra = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Compra.Compra>();
    Verificar(System.Net.WebUtility.HtmlDecode(compra.ToHtmlString()).Contains("Sin cupón") && seleccion.IdCupon == 0, "La compra no permite continuar sin cupón");
    var pantallaCompra = componentes.Compra!;
    var seleccionar = typeof(Web.Componentes.Paginas.Compra.Compra).GetMethod("Seleccionar",
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
    fallarSeleccion = true;
    await (Task)seleccionar.Invoke(pantallaCompra, [9, 4, 0])!;
    typeof(ComponentBase).GetMethod("StateHasChanged", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(pantallaCompra, null);
    string compraRechazada = System.Net.WebUtility.HtmlDecode(compra.ToHtmlString());
    Verificar(seleccion.IdDireccionEnvio == 3 && compraRechazada.Split("selected").Length == 4
        && compraRechazada.Contains("Selección rechazada."), "Una selección rechazada altera las opciones confirmadas u oculta el error");
    fallarSeleccion = false;
    await (Task)seleccionar.Invoke(pantallaCompra, [3, 4, 0])!;
    Verificar(compraServicio.Resumen?.Total == 84 && seleccion.IdCupon == 0, "Seleccionar altera los importes o la opción sin cupón");
    var confirmar = typeof(Web.Componentes.Paginas.Compra.Compra).GetMethod("Confirmar",
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
    rechazarPago = true;
    await (Task)confirmar.Invoke(pantallaCompra, null)!;
    Verificar(compraServicio.Resumen?.Total == 94.5m && compraServicio.CantidadCarrito == 2,
        "El rechazo no recupera los importes vigentes o vacía el carrito");
    rechazarPago = false;
    await (Task)confirmar.Invoke(pantallaCompra, null)!;
    Verificar(compraServicio.CantidadCarrito == 0, "El pago confirmado deja el carrito lleno");
    resumenPreparacion = resumen;

    var miPerfil = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MiPerfil>();
    var pantallaPerfil = componentes.Perfil!;
    var clasePerfil = typeof(Web.Componentes.Paginas.Cuenta.MiPerfil);
    var guardarFoto = clasePerfil.GetMethod("GuardarFoto", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
    clasePerfil.GetField("urlFoto", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .SetValue(pantallaPerfil, "https://example.test/foto.jpg");
    fallarFoto = true;
    await (Task)guardarFoto.Invoke(pantallaPerfil, [false])!;
    Verificar(estado.Usuario!.Imagen == "/images/user-placeholder.svg", "Un fallo de foto reemplaza la imagen anterior");
    fallarFoto = false;
    await (Task)guardarFoto.Invoke(pantallaPerfil, [false])!;
    Verificar(estado.Usuario!.Imagen == "https://example.test/foto.jpg", "Reintentar no guarda la foto elegida");
    await (Task)guardarFoto.Invoke(pantallaPerfil, [true])!;
    Verificar(estado.Usuario!.Imagen == "/images/user-placeholder.svg", "Quitar foto no recupera la imagen genérica");

    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisSeguimientos>();
    var claseSeguimientos = typeof(Web.Componentes.Paginas.Cuenta.MisSeguimientos);
    await (Task)claseSeguimientos.GetMethod("MostrarTipo", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(componentes.Seguimientos, ["categorias"])!;
    await (Task)claseSeguimientos.GetMethod("CambiarPagina", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(componentes.Seguimientos, [2])!;
    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisSeguimientos>();
    Verificar(ultimaConsultaSeguimientos?.Tipo == "CATEGORIA" && ultimaConsultaSeguimientos.Pagina == 2,
        "Volver a seguimientos pierde la pestaña o la página elegidas");

    var confirmacion = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Compra.DetallePedido>(
        ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 21 }));
    Verificar(confirmacion.ToHtmlString().Contains("SIM-21"), "La confirmación no recupera el pedido desde REST sin una copia en memoria");
    string htmlConfirmacion = System.Net.WebUtility.HtmlDecode(confirmacion.ToHtmlString());
    Verificar(htmlConfirmacion.Contains("S/ 84.00") && htmlConfirmacion.Contains("S/ 80.00")
        && htmlConfirmacion.Contains("****1234") && htmlConfirmacion.Contains("Calle 1"),
        "La confirmación pierde los importes, dirección o máscara recibidos");

    estado.LibroComparacion = null;
    var sinComparacion = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Comparacion>();
    Verificar(sinComparacion.ToHtmlString().Contains("Elige dos lecturas"), "La comparación vacía no permite empezar");
    estado.LibroComparacion = new Web.Modelos.Libro { Id = 7 };
    var comparacion = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Comparacion>();
    string htmlComparacion = System.Net.WebUtility.HtmlDecode(comparacion.ToHtmlString());
    Verificar(htmlComparacion.Contains("Segundo libro") && htmlComparacion.Contains("2 reseñas")
        && htmlComparacion.Contains("Sin reseñas") && htmlComparacion.Contains("S/ 40.00"),
        "La comparación pierde libros, valoración o precio con descuento");
    var valoracion = new ValoracionLibroDTO(7, 1, 0, 0, 0, 1);
    Verificar(valoracion.CantidadResenas == 2 && valoracion.Promedio == 3,
        "El resumen de estrellas no calcula cantidad y promedio sobre todas las puntuaciones");

    var formularioDireccion = await renderer.RenderComponentAsync<Web.Componentes.Compartidos.FormularioDireccion>();
    string htmlDireccion = formularioDireccion.ToHtmlString();
    Verificar(!htmlDireccion.Contains("novalidate") && htmlDireccion.Split("required").Length == 5,
        "La dirección no exige alias, calle, distrito y ciudad en el formulario");
    var formularioPago = await renderer.RenderComponentAsync<Web.Componentes.Compartidos.FormularioPago>();
    Verificar(formularioPago.ToHtmlString().Split("required").Length == 5,
        "La tarjeta nueva no exige titular, proveedor, número y vencimiento");
    var editarPago = await renderer.RenderComponentAsync<Web.Componentes.Compartidos.FormularioPago>(
        ParameterView.FromDictionary(new Dictionary<string, object?> { ["Valor"] = cuentaServicio.Metodos.Single() }));
    Verificar(editarPago.ToHtmlString().Split("required").Length == 4,
        "Editar un método exige volver a ingresar el número registrado");
    var inicio = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Inicio>();
    Verificar(inicio.ToHtmlString().Contains("Libro real"), "Inicio no muestra datos REST");
    Verificar(catalogoServicio.Idiomas.Count == 13 && catalogoServicio.Idiomas.Contains("Español"), "No se cargaron todas las páginas de idiomas");
    var catalogo = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Busqueda"] = "historia", ["OrdenInicial"] = "MAS_VENDIDOS" }));
    Verificar(catalogo.ToHtmlString().Contains("Libro real") && ultimoOrden == "MAS_VENDIDOS", "Catálogo no envía el orden al servidor");
    Verificar(!catalogo.ToHtmlString().Contains("Aplicar filtros") && !catalogo.ToHtmlString().Contains("Limpiar filtros"), "Aún aparecen acciones manuales de filtros");
    var explorador = componentes.Explorador!;
    // Ejecuta los mismos manejadores de las casillas, sin exponer métodos de prueba en Web.
    async Task Cambiar(string metodo, params object[] argumentos)
    {
        var accion = typeof(Web.Componentes.Paginas.Catalogo.ExploradorCatalogo).GetMethod(metodo,
            System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
        await (Task)accion.Invoke(explorador, argumentos)!;
    }
    async Task SeleccionarFiltro(string grupo, string valor)
    {
        var vista = typeof(Web.Componentes.Paginas.Catalogo.ExploradorCatalogo).GetField("vista",
            System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.GetValue(explorador)!;
        await Cambiar("SeleccionarFiltro", vista.GetType().GetProperty(grupo)!.GetValue(vista)!, valor);
    }
    await Cambiar("CambiarPagina", 3);
    await SeleccionarFiltro("Categorias", "Referencia real");
    Verificar(ultimoFiltro!.IdsCategorias.SequenceEqual(new[] { 1 }) && ultimoFiltro.Pagina == 1
        && ultimoFiltro.Titulo == "historia" && ultimoFiltro.Orden == "MAS_VENDIDOS", "Seleccionar categoría no consulta inmediatamente conservando búsqueda y orden");
    await SeleccionarFiltro("Autores", "Referencia real");
    await SeleccionarFiltro("Editoriales", "Referencia real");
    await SeleccionarFiltro("Idiomas", "Español");
    Verificar(ultimoFiltro!.IdsCategorias.Count == 1 && ultimoFiltro.IdsAutores.Count == 1
        && ultimoFiltro.IdsEditoriales.Count == 1 && ultimoFiltro.Idiomas.Contains("Español"), "Los filtros automáticos no se combinan");
    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>(ParameterView.FromDictionary(
        new Dictionary<string, object?> { ["Busqueda"] = "historia", ["OrdenInicial"] = "MAS_VENDIDOS" }));
    explorador = componentes.Explorador!;
    Verificar(ultimoFiltro!.IdsCategorias.Count == 1 && ultimoFiltro.Idiomas.Contains("Español"), "Se pierden los filtros al volver a la lista");
    await SeleccionarFiltro("Categorias", "Referencia real");
    Verificar(ultimoFiltro!.IdsCategorias.Count == 0 && ultimoFiltro.IdsAutores.Count == 1, "Quitar una categoría borra otros filtros");
    var vistaCatalogo = typeof(Web.Componentes.Paginas.Catalogo.ExploradorCatalogo).GetField("vista",
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.GetValue(explorador)!;
    vistaCatalogo.GetType().GetProperty("Maximo")!.SetValue(vistaCatalogo, 45.50m);
    await Cambiar("AjustarMaximo");
    Verificar(ultimoFiltro!.PrecioMaximo == 45.50m && ultimoFiltro.Pagina == 1, "El precio no consulta inmediatamente con céntimos");
    await Cambiar("QuitarPrecio");
    Verificar(ultimoFiltro!.PrecioMinimo is null && ultimoFiltro.PrecioMaximo is null, "Quitar precio deja límites anteriores");
    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>(ParameterView.FromDictionary(
        new Dictionary<string, object?> { ["TipoAmbito"] = "autores", ["IdAmbito"] = 9 }));
    explorador = componentes.Explorador!;
    await SeleccionarFiltro("Idiomas", "Español");
    Verificar(ultimaRutaLibros == "/webresources/libros/por-autor/9", "Cambiar filtros abandona la referencia fija");
    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.ExploradorCatalogo>(ParameterView.FromDictionary(
        new Dictionary<string, object?> { ["Favoritos"] = true }));
    explorador = componentes.Explorador!;
    await SeleccionarFiltro("Idiomas", "Español");
    Verificar(ultimaRutaLibros == "/webresources/cuenta/libros-guardados/busqueda?favorito=true", "Cambiar filtros abandona la lista personal");
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
    Verificar(html.Contains("/libros/7#resenas"), "Ver reseñas abandona la ruta del libro");
    Verificar(html.Contains("Sinopsis real") && html.Contains("Reseña real") && html.Contains("/referencias/categorias/1"), "Detalle no representa la respuesta REST");
    Verificar(html.Contains("Lector real") && html.Contains("2 votos útiles") && html.Contains("/images/user-placeholder.svg"), "Se perdieron el autor, los votos o la foto genérica de la reseña");
    var alternarGuardado = typeof(Web.Componentes.Paginas.Catalogo.DetalleLibro).GetMethod("AlternarGuardado",
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
    await (Task)alternarGuardado.Invoke(componentes.Libro!, [false])!;
    Verificar(deseado && !favorito, "Guardar un deseado modifica favoritos");
    await (Task)alternarGuardado.Invoke(componentes.Libro!, [true])!;
    Verificar(deseado && favorito, "Guardar un favorito pierde el deseado");
    await (Task)alternarGuardado.Invoke(componentes.Libro!, [false])!;
    Verificar(!deseado && favorito, "Retirar de deseados altera favoritos");
    estado.Usuario = null;
    await (Task)alternarGuardado.Invoke(componentes.Libro!, [true])!;
    Verificar(escriturasGuardados == 3 && favorito, "Sin sesión se intenta modificar una lista privada");
    estado.MostrarPerfil(perfil);
});
await renderer.Dispatcher.InvokeAsync(async () =>
{
    foreach (string tipo in new[] { "autores", "categorias", "editoriales" })
    {
        var parametros = ParameterView.FromDictionary(new Dictionary<string, object?> { ["Tipo"] = tipo });
        var pantalla = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Referencias>(parametros);
        var referencia = componentes.Referencias!;
        var clase = typeof(Web.Componentes.Paginas.Catalogo.Referencias);
        async Task Ejecutar(string metodo, params object[] argumentos)
        {
            await (Task)clase.GetMethod(metodo, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
                .Invoke(referencia, argumentos)!;
        }
        void Escribir(string campo, string valor) => clase.GetField(campo,
            System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.SetValue(referencia, valor);
        Verificar(pantalla.ToHtmlString().Contains("Buscar por nombre") && !pantalla.ToHtmlString().Contains("<aside"), "El buscador no aparece en la cabecera o añade una columna lateral");
        await Ejecutar("CambiarPagina", 2);
        Escribir("textoBusqueda", "  Referencia  ");
        await Ejecutar("Buscar");
        Verificar(ultimoFiltroReferencia!.Nombre == "Referencia" && ultimoFiltroReferencia.Pagina == 1
            && ultimaRutaReferencia == $"/webresources/{tipo}/busqueda", "La búsqueda no se envía al listado correcto ni reinicia su página");
        var vistaReferencias = clase.GetField("vista", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
            .GetValue(referencia)!;
        vistaReferencias.GetType().GetProperty("Orden")!.SetValue(vistaReferencias, "reciente");
        await Ejecutar("ActualizarResultados");
        await Ejecutar("CambiarPagina", 2);
        Verificar(ultimoFiltroReferencia!.Nombre == "Referencia" && ultimoFiltroReferencia.Pagina == 2
            && ultimoFiltroReferencia.Orden == "MAS_RECIENTES", "Ordenar o paginar pierde la búsqueda");
        await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Referencias>(parametros);
        referencia = componentes.Referencias!;
        Verificar(ultimoFiltroReferencia!.Nombre == "Referencia" && ultimoFiltroReferencia.Pagina == 2
            && ultimoFiltroReferencia.Orden == "MAS_RECIENTES", "Volver al listado pierde la búsqueda, orden o página");
        await Ejecutar("QuitarNombre");
        Verificar(ultimoFiltroReferencia!.Nombre == "" && ultimoFiltroReferencia.Pagina == 1, "Quitar la búsqueda no recupera el listado");
        Escribir("textoBusqueda", "inexistente");
        await Ejecutar("Buscar");
        var vacia = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Referencias>(parametros);
        Verificar(vacia.ToHtmlString().Contains("No encontramos coincidencias") && vacia.ToHtmlString().Contains("Buscar por nombre"), "No se puede corregir una búsqueda sin coincidencias");
    }
});
await servidor.StopAsync();
Console.WriteLine($"Correcto: {verificaciones} comprobaciones de contrato HTTP, JSON, cookies y compra.");

class NavegacionPrueba : NavigationManager
{
    public NavegacionPrueba() => Initialize("http://localhost/", "http://localhost/catalogo");
    protected override void NavigateToCore(string uri, bool forceLoad) { }
}

class ComponentesPrueba : IComponentActivator
{
    public Web.Componentes.Paginas.Catalogo.ExploradorCatalogo? Explorador { get; private set; }
    public Web.Componentes.Paginas.Catalogo.Referencias? Referencias { get; private set; }
    public Web.Componentes.Paginas.Compra.Compra? Compra { get; private set; }
    public Web.Componentes.Paginas.Cuenta.MiPerfil? Perfil { get; private set; }
    public Web.Componentes.Paginas.Catalogo.DetalleLibro? Libro { get; private set; }
    public Web.Componentes.Paginas.Cuenta.MisSeguimientos? Seguimientos { get; private set; }
    public IComponent CreateInstance(Type tipo)
    {
        var componente = (IComponent)Activator.CreateInstance(tipo)!;
        if (componente is Web.Componentes.Paginas.Catalogo.Comparacion comparacion)
            comparacion.IdSegundoLibro = 8;
        if (componente is Web.Componentes.Paginas.Catalogo.ExploradorCatalogo explorador)
            Explorador = explorador;
        if (componente is Web.Componentes.Paginas.Catalogo.Referencias referencias)
            Referencias = referencias;
        if (componente is Web.Componentes.Paginas.Compra.Compra compra)
            Compra = compra;
        if (componente is Web.Componentes.Paginas.Cuenta.MiPerfil perfil)
            Perfil = perfil;
        if (componente is Web.Componentes.Paginas.Catalogo.DetalleLibro libro)
            Libro = libro;
        if (componente is Web.Componentes.Paginas.Cuenta.MisSeguimientos seguimientos)
            Seguimientos = seguimientos;
        return componente;
    }
}
