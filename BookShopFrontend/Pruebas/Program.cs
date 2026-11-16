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
using Microsoft.JSInterop;
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
bool fallarDocumento = false;
var solicitudesDocumentos = new List<string>();
string? cookieDocumento = null;
byte[] pdfPrueba = System.Text.Encoding.ASCII.GetBytes("%PDF-1.4\nPDF de prueba\n%%EOF");
bool deseado = false, favorito = false;
int escriturasGuardados = 0;
bool historialSuficiente = false;
int aperturasRecomendaciones = 0;
string? ultimaRutaRecomendaciones = null, ultimaConsultaRecomendaciones = null;
ConsultaRecomendacionesDTO? ultimoOrdenRecomendaciones = null;
var consultasDescartadas = new List<string>();
bool resenaPublicada = false, votoUtilPropio = false, reportePendiente = false, fallarGuardarResena = false;
int comprobacionesPublicacion = 0, publicacionesResena = 0, enviosReporte = 0;
DatosResenaDTO contenidoResena = new() { Puntuacion = 4, Comentario = "Mi opinión" };
DatosReporteResenaDTO? ultimoReporteResena = null;
ConsultaResenasDTO? ultimaConsultaResenas = null;
FiltroPedidoDTO? ultimoFiltroPedido = null;
DatosCancelacionDTO? ultimaCancelacion = null;
DatosDevolucionDTO? ultimaDevolucion = null;
int consultasPedidos = 0, lecturasPedido = 0, actualizacionesSeguimiento = 0, permisosDevolucion = 0;
bool avanzarSeguimiento = false, rechazarCancelacion = true, devolucionPendiente = false;
FiltroSolicitudDTO? ultimoFiltroSolicitud = null;
string? ultimoAmbitoSolicitud = null;
int consultasSolicitudes = 0;
bool fallarEdicionSolicitud = false, devolucionRetirada = false;
var solicitudDevolucion = new DetalleDevolucionDTO
{
    Solicitud = new ResumenSolicitudDTO { Id = 31, Tipo = "DEVOLUCION", Estado = "PENDIENTE",
        FechaSolicitud = new DateTime(2026, 11, 10, 9, 0, 0), NombreSolicitante = "Ana", Motivo = "Libro dañado", IdReferencia = 21 },
    Datos = new DatosDevolucionDTO { Motivo = "LIBRO_DANADO", Comentario = "La cubierta llegó rota" },
    Pedido = new ResumenPedidoDTO(21, new DateTime(2026, 11, 9), "ENTREGADO", 2, 84)
};
var solicitudReporte = new DetalleReporteResenaDTO
{
    Solicitud = new ResumenSolicitudDTO { Id = 32, Tipo = "REPORTE_RESENA", Estado = "PENDIENTE",
        FechaSolicitud = new DateTime(2026, 11, 10, 10, 0, 0), NombreSolicitante = "Ana", Motivo = "Publicidad o spam", IdReferencia = 1 },
    Datos = new DatosReporteResenaDTO { Motivo = "PUBLICIDAD_O_SPAM", Comentario = "Comentario privado del reporte" },
    ResenaReportada = new ResenaReportadaDTO { IdResena = 1, NombreUsuario = "Lector", TituloLibro = "Libro real",
        ContenidoOriginal = new DatosResenaDTO { Puntuacion = 1, Comentario = "Contenido original denunciado" } },
    ContenidoActual = new DatosResenaDTO { Puntuacion = 3, Comentario = "Contenido actual editado" }, ResenaActiva = true
};
void Verificar(bool condicion, string mensaje)
{
    if (!condicion) throw new Exception(mensaje);
    verificaciones++;
}
var resumen = new ResumenCompraDTO([new ItemDTO(7, "Libro real", null, 2, 50, 40, 20, 80, true, 10)], 100, 20, 0, 0, 0, 5, 4, 84, 2);
DetallePedidoDTO CrearPedido(string estado, int id = 21) => new(
    new(id, new DateTime(2026, 11, 9, 10, 0, 0), estado, 2, 84), resumen,
    new(3, "Casa", "Calle 1", "Centro", "Lima", "", true),
    new(4, true, "Ana", "Banco", "Tarjeta", "****1234", "DEBITO", new DateOnly(2030, 1, 1), true, true),
    $"SIM-{id}", $"BS-{id}", new DateTime(2026, 11, 9, 10, 1, 30),
    estado is "EN_CAMINO" or "ENTREGADO" ? new DateTime(2026, 11, 9, 10, 0, 30) : null,
    estado == "ENTREGADO" ? new DateTime(2026, 11, 9, 10, 1, 30) : null,
    estado == "EN_ALMACEN", estado == "ENTREGADO" && !devolucionPendiente);
var pedidoPrueba = CrearPedido("EN_ALMACEN");
ResumenCompraDTO resumenPreparacion = resumen;
var perfil = new PerfilUsuarioDTO(new TarjetaUsuarioDTO(1, "Ana", "ana@correo.pe", null), new DateOnly(2000, 1, 2));
var seleccion = new SeleccionCompraDTO(3, 4, 0);
servidor.Map("/{**ruta}", async contexto =>
{
    var ruta = contexto.Request.Path.Value!;
    object? resultado = null;
    if (ruta.StartsWith("/webresources/documentos/"))
    {
        solicitudesDocumentos.Add(ruta + contexto.Request.QueryString);
        cookieDocumento = contexto.Request.Cookies["JSESSIONID"];
        Verificar(contexto.Request.Headers.Accept.ToString().Contains("application/pdf"), "El cliente no solicita PDF");
        if (fallarDocumento)
        {
            contexto.Response.StatusCode = 400;
            await contexto.Response.WriteAsJsonAsync(new { codigo = "Validacion", mensaje = "El período no es válido." });
        }
        else
        {
            contexto.Response.ContentType = "application/pdf";
            await contexto.Response.Body.WriteAsync(pdfPrueba);
        }
        return;
    }
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
    else if (ruta == "/webresources/solicitudes/propias/busqueda" || ruta == "/webresources/solicitudes/moderacion/busqueda")
    {
        consultasSolicitudes++;
        ultimoAmbitoSolicitud = ruta;
        ultimoFiltroSolicitud = (await contexto.Request.ReadFromJsonAsync<FiltroSolicitudDTO>())!;
        var datos = new List<ResumenSolicitudDTO>();
        if (ultimoFiltroSolicitud.Tipo == "DEVOLUCION" && !devolucionRetirada)
            datos.Add(solicitudDevolucion.Solicitud);
        if (ultimoFiltroSolicitud.Tipo == "REPORTE_RESENA")
            datos.Add(solicitudReporte.Solicitud);
        if (ultimoFiltroSolicitud.Estados.Count > 0)
            datos = datos.Where(s => ultimoFiltroSolicitud.Estados.Contains(s.Estado)).ToList();
        resultado = new ResultadoPaginadoDTO<ResumenSolicitudDTO>(datos, datos.Count, ultimoFiltroSolicitud.Pagina, 10);
    }
    else if (ruta == "/webresources/solicitudes/devoluciones/31/resolucion")
    {
        solicitudDevolucion.Solicitud.Estado = contexto.Request.Query["resultado"]!;
        solicitudDevolucion.FechaResolucion = new DateTime(2026, 11, 11, 12, 0, 0);
        if (solicitudDevolucion.Solicitud.Estado == "ACEPTADA")
            solicitudDevolucion.Pedido.Estado = "DEVUELTO";
        resultado = solicitudDevolucion;
    }
    else if (ruta == "/webresources/solicitudes/reportes/32/resolucion")
    {
        solicitudReporte.Solicitud.Estado = contexto.Request.Query["resultado"]!;
        solicitudReporte.FechaResolucion = new DateTime(2026, 11, 11, 12, 0, 0);
        resultado = solicitudReporte;
    }
    else if (ruta == "/webresources/solicitudes/devoluciones/31") resultado = solicitudDevolucion;
    else if (ruta == "/webresources/solicitudes/reportes/32")
    {
        if (contexto.Request.Method == "PUT")
        {
            if (fallarEdicionSolicitud)
            {
                contexto.Response.StatusCode = 400;
                await contexto.Response.WriteAsJsonAsync(new { mensaje = "No se pudo guardar el reporte." });
                return;
            }
            solicitudReporte.Datos = (await contexto.Request.ReadFromJsonAsync<DatosReporteResenaDTO>())!;
        }
        resultado = solicitudReporte;
    }
    else if (ruta == "/webresources/solicitudes/31" && contexto.Request.Method == "DELETE")
    {
        devolucionRetirada = true;
        contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/solicitudes/31/pedido") resultado = CrearPedido(solicitudDevolucion.Pedido.Estado);
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
    {
        lecturasPedido++;
        resultado = pedidoPrueba;
    }
    else if (ruta == "/webresources/pedidos/22")
    {
        lecturasPedido++;
        resultado = CrearPedido("EN_ALMACEN", 22);
    }
    else if (ruta == "/webresources/pedidos/busqueda")
    {
        consultasPedidos++;
        ultimoFiltroPedido = (await contexto.Request.ReadFromJsonAsync<FiltroPedidoDTO>())!;
        resultado = new ResultadoPaginadoDTO<TarjetaPedidoDTO>([
            new() { Pedido = pedidoPrueba.Pedido,
                Portadas = ["/portada1.png", null, "/portada3.png", "/portada4.png", "/portada5.png", "/portada6.png"] }
        ], 51, ultimoFiltroPedido.Pagina, ultimoFiltroPedido.TamanioPagina);
    }
    else if (ruta == "/webresources/pedidos/21/seguimiento")
    {
        actualizacionesSeguimiento++;
        Verificar(contexto.Request.Method == "PUT", "Consultar avanza el seguimiento sin pulsar Actualizar");
        if (avanzarSeguimiento)
            pedidoPrueba = CrearPedido(pedidoPrueba.Pedido.Estado == "EN_ALMACEN" ? "EN_CAMINO" : "ENTREGADO");
        resultado = pedidoPrueba;
    }
    else if (ruta == "/webresources/pedidos/21/cancelacion")
    {
        ultimaCancelacion = await contexto.Request.ReadFromJsonAsync<DatosCancelacionDTO>();
        if (rechazarCancelacion)
        {
            contexto.Response.StatusCode = 409;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "La cancelación simulada fue rechazada. Inténtalo de nuevo." });
            return;
        }
        pedidoPrueba = CrearPedido("CANCELADO");
        resultado = pedidoPrueba;
    }
    else if (ruta == "/webresources/solicitudes/devoluciones/pedido/21/permitido")
    {
        permisosDevolucion++;
        if (devolucionPendiente)
        {
            contexto.Response.StatusCode = 409;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "Ya hay una devolución pendiente para este pedido." });
        }
        else
            contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/solicitudes/devoluciones/pedido/21")
    {
        ultimaDevolucion = await contexto.Request.ReadFromJsonAsync<DatosDevolucionDTO>();
        devolucionPendiente = true;
        pedidoPrueba = CrearPedido("ENTREGADO");
        resultado = new { solicitud = new { id = 78 } };
    }
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
    else if (ruta == "/webresources/recomendaciones/para-ti/disponible") resultado = historialSuficiente;
    else if (ruta == "/webresources/recomendaciones/populares" || ruta == "/webresources/recomendaciones/para-ti")
    {
        var consulta = (await contexto.Request.ReadFromJsonAsync<ConsultaRecomendacionesDTO>())!;
        if (ruta.EndsWith("/para-ti") && !historialSuficiente)
        {
            contexto.Response.StatusCode = 422;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "Necesitas al menos tres pedidos para ver recomendaciones." });
            return;
        }
        aperturasRecomendaciones++;
        ultimaRutaRecomendaciones = ruta;
        ultimoOrdenRecomendaciones = consulta;
        string tipo = ruta.EndsWith("/para-ti") ? "personal" : "popular";
        resultado = new RecomendacionesDTO
        {
            IdConsulta = tipo + aperturasRecomendaciones,
            Resultados = new ResultadoPaginadoDTO<TarjetaLibroDTO>(
                [new(9, "Recomendación " + tipo, null, "Autor", 50, 40, 20, true)], 25, consulta.Pagina, consulta.TamanioPagina)
        };
    }
    else if (ruta.StartsWith("/webresources/recomendaciones/consultas/"))
    {
        string id = ruta.Split('/').Last();
        if (contexto.Request.Method == "DELETE")
        {
            consultasDescartadas.Add(id);
            contexto.Response.StatusCode = 204;
            return;
        }
        ultimoOrdenRecomendaciones = (await contexto.Request.ReadFromJsonAsync<ConsultaRecomendacionesDTO>())!;
        ultimaConsultaRecomendaciones = id;
        string tipo = id.StartsWith("personal") ? "personal" : "popular";
        resultado = new ResultadoPaginadoDTO<TarjetaLibroDTO>(
            [new(9, "Recomendación " + tipo, null, "Autor", 50, 35, 30, true)], 25,
            ultimoOrdenRecomendaciones.Pagina, ultimoOrdenRecomendaciones.TamanioPagina);
    }
    else if (ruta == "/webresources/recomendaciones/similares/7") resultado = Array.Empty<TarjetaLibroDTO>();
    else if (ruta == "/webresources/resenas/libro/7/publicacion/permitida")
    {
        comprobacionesPublicacion++;
        if (resenaPublicada)
        {
            contexto.Response.StatusCode = 409;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "Ya tienes una reseña publicada para este libro." });
        }
        else
            contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/resenas/libro/7")
    {
        if (fallarGuardarResena)
        {
            contexto.Response.StatusCode = 400;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "No se pudo guardar la reseña." });
            return;
        }
        contenidoResena = (await contexto.Request.ReadFromJsonAsync<DatosResenaDTO>())!;
        resenaPublicada = true;
        publicacionesResena++;
        resultado = 2;
    }
    else if (ruta == "/webresources/resenas/2")
    {
        if (contexto.Request.Method == "DELETE")
            resenaPublicada = false;
        else
            contenidoResena = (await contexto.Request.ReadFromJsonAsync<DatosResenaDTO>())!;
        contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/resenas/1/voto")
    {
        votoUtilPropio = contexto.Request.Method == "PUT";
        resultado = true;
    }
    else if (ruta == "/webresources/solicitudes/reportes/resena/1/permitido")
    {
        if (reportePendiente)
        {
            contexto.Response.StatusCode = 409;
            await contexto.Response.WriteAsJsonAsync(new { mensaje = "Ya tienes un reporte pendiente." });
        }
        else
            contexto.Response.StatusCode = 204;
        return;
    }
    else if (ruta == "/webresources/solicitudes/reportes/resena/1")
    {
        ultimoReporteResena = await contexto.Request.ReadFromJsonAsync<DatosReporteResenaDTO>();
        enviosReporte++;
        reportePendiente = true;
        resultado = new { solicitud = new { id = 77 } };
    }
    else if (ruta == "/webresources/resenas/libro/7/valoracion")
        resultado = new ValoracionLibroDTO(7,
            resenaPublicada && contenidoResena.Puntuacion == 1 ? 1 : 0,
            resenaPublicada && contenidoResena.Puntuacion == 2 ? 1 : 0,
            resenaPublicada && contenidoResena.Puntuacion == 3 ? 1 : 0,
            resenaPublicada && contenidoResena.Puntuacion == 4 ? 1 : 0,
            1 + (resenaPublicada && contenidoResena.Puntuacion == 5 ? 1 : 0));
    else if (ruta == "/webresources/resenas/libro/7/busqueda" || ruta == "/webresources/resenas/propias/busqueda")
    {
        var consulta = (await contexto.Request.ReadFromJsonAsync<ConsultaResenasDTO>())!;
        bool propias = ruta.Contains("/propias/");
        if (propias)
            ultimaConsultaResenas = consulta;
        var filas = new List<ResenaDTO>();
        if (!propias)
            filas.Add(new(1, 7, "Libro real", null, "Lector real", null, 5, "Reseña real",
                new DateTime(2026, 11, 9), 2 + (votoUtilPropio ? 1 : 0), votoUtilPropio, true));
        if (resenaPublicada)
            filas.Add(new(2, 7, "Libro real", "/portada-propia.png", "Ana", "/foto-propia.png",
                contenidoResena.Puntuacion, contenidoResena.Comentario, new DateTime(2026, 11, 16), 0, false, true));
        resultado = new ResultadoPaginadoDTO<ResenaDTO>(filas,
            propias && resenaPublicada ? 21 : filas.Count, consulta.Pagina, consulta.TamanioPagina);
    }
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
var archivoPdf = await api.GetArchivo("documentos/comprobantes/21");
Verificar(archivoPdf.SequenceEqual(pdfPrueba) && cookieDocumento == "sesion-ana",
    "La descarga binaria altera el PDF o pierde la cookie del usuario");
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
var navegadorPdf = new NavegadorPdfPrueba();
var documentos = new DocumentoServicio(api, navegadorPdf);
servicios.AddSingleton<IComponentActivator>(componentes);
servicios.AddLogging(); servicios.AddSingleton(estado);
servicios.AddSingleton<IJSRuntime>(navegadorPdf);
servicios.AddSingleton(documentos);
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

    var pantallaPedido = componentes.Pedido!;
    var generarComprobante = pantallaPedido.GetType().GetMethod("DescargarComprobante",
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!;
    await (Task)generarComprobante.Invoke(pantallaPedido, null)!;
    Verificar(navegadorPdf.NombreArchivo == "comprobante-pedido-21.pdf" && navegadorPdf.Contenido.SequenceEqual(pdfPrueba),
        "La descarga del comprobante altera el PDF o su nombre");
    fallarDocumento = true;
    await (Task)generarComprobante.Invoke(pantallaPedido, null)!;
    typeof(ComponentBase).GetMethod("StateHasChanged", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(pantallaPedido, null);
    Verificar(System.Net.WebUtility.HtmlDecode(confirmacion.ToHtmlString()).Contains("El período no es válido.") && confirmacion.ToHtmlString().Contains("SIM-21"),
        "El error del comprobante oculta la compra confirmada o el mensaje REST");
    fallarDocumento = false;
    await (Task)generarComprobante.Invoke(pantallaPedido, null)!;
    Verificar(navegadorPdf.Contenido.SequenceEqual(pdfPrueba), "El comprobante no permite reintentar después de un error");

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
await renderer.Dispatcher.InvokeAsync(async () =>
{
    estado.LimpiarCuenta();
    var visitante = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Administracion.Administracion>();
    Verificar(System.Net.WebUtility.HtmlDecode(visitante.ToHtmlString()).Contains("Inicia sesión") && !visitante.ToHtmlString().Contains(">Reportes</button>"),
        "La URL de administración permite generar reportes sin sesión");
    estado.MostrarPerfil(perfil, false);
    var cliente = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Administracion.Administracion>();
    Verificar(cliente.ToHtmlString().Contains("Solo los administradores") && !cliente.ToHtmlString().Contains(">Reportes</button>"),
        "La URL de administración muestra acciones a un cliente");
    var cuenta = await renderer.RenderComponentAsync<Web.Componentes.Estructura.EstructuraPrincipal>();
    var pantallaCuenta = componentes.Estructura!;
    pantallaCuenta.GetType().GetField("cuentaAbierta", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .SetValue(pantallaCuenta, true);
    typeof(ComponentBase).GetMethod("StateHasChanged", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(pantallaCuenta, null);
    Verificar(!System.Net.WebUtility.HtmlDecode(cuenta.ToHtmlString()).Contains("Administración"),
        "Cuenta muestra Administración a un cliente");
    estado.MostrarPerfil(perfil, true);
    typeof(ComponentBase).GetMethod("StateHasChanged", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(pantallaCuenta, null);
    string htmlCuenta = System.Net.WebUtility.HtmlDecode(cuenta.ToHtmlString());
    Verificar(htmlCuenta.Contains("Administración") && !htmlCuenta.Contains("Reportes"),
        "Cuenta no ofrece Administración o añade Reportes fuera de ella");
    var administracion = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Administracion.Administracion>();
    var paginaAdministracion = componentes.Administracion!;
    paginaAdministracion.GetType().GetField("reportesAbiertos", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .SetValue(paginaAdministracion, true);
    typeof(ComponentBase).GetMethod("StateHasChanged", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!
        .Invoke(paginaAdministracion, null);
    string htmlAdministracion = System.Net.WebUtility.HtmlDecode(administracion.ToHtmlString());
    Verificar(htmlAdministracion.Contains("role=\"dialog\"") && htmlAdministracion.Contains("Libros</button>")
        && htmlAdministracion.Contains("Usuarios</button>"), "Reportes no se abre como modal con dos pestañas");
    Verificar(typeof(Web.Componentes.Paginas.Administracion.ModalReportes).GetCustomAttributes(typeof(RouteAttribute), false).Length == 0,
        "El modal de reportes expone una página independiente");
    var reportes = componentes.Reportes!;
    var tipoReportes = reportes.GetType();
    var privados = System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic;
    tipoReportes.GetField("inicio", privados)!.SetValue(reportes, new DateTime(2026, 1, 1));
    tipoReportes.GetField("fin", privados)!.SetValue(reportes, new DateTime(2026, 1, 31));
    await (Task)tipoReportes.GetMethod("Generar", privados)!.Invoke(reportes, null)!;
    Verificar(solicitudesDocumentos.Last().EndsWith("ventas-libros?inicio=2026-01-01&fin=2026-01-31")
        && navegadorPdf.NombreArchivo == "reporte-ventas-libros-2026-01-01-2026-01-31.pdf",
        "El reporte de libros no conserva el período o la opción sin límite");
    tipoReportes.GetField("pestana", privados)!.SetValue(reportes, "usuarios");
    tipoReportes.GetField("mes", privados)!.SetValue(reportes, new DateTime(2026, 2, 1));
    tipoReportes.GetField("cantidadUsuarios", privados)!.SetValue(reportes, 5);
    await (Task)tipoReportes.GetMethod("Generar", privados)!.Invoke(reportes, null)!;
    Verificar(solicitudesDocumentos.Last().EndsWith("usuarios?mes=2026-02&cantidad=5")
        && navegadorPdf.NombreArchivo == "reporte-usuarios-2026-02.pdf", "El reporte de usuarios altera el mes o máximo");
    int antesDeCancelar = solicitudesDocumentos.Count;
    await reportes.AlCerrar.InvokeAsync();
    Verificar(solicitudesDocumentos.Count == antesDeCancelar && !administracion.ToHtmlString().Contains("role=\"dialog\""),
        "Cancelar genera un documento o deja abierto el modal");
    estado.MostrarPerfil(perfil, false);
});
await renderer.Dispatcher.InvokeAsync(async () =>
{
    var navegacion = (NavegacionPrueba)proveedor.GetRequiredService<NavigationManager>();
    navegacion.Ir("/populares");
    var populares = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Recomendaciones>();
    var pantalla = componentes.Recomendaciones.Last();
    var privados = System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic;
    var tipo = pantalla.GetType();
    string id = (string)tipo.GetField("idConsulta", privados)!.GetValue(pantalla)!;
    int aperturas = aperturasRecomendaciones;
    var consulta = (ConsultaRecomendacionesDTO)tipo.GetField("consulta", privados)!.GetValue(pantalla)!;
    Verificar(consulta.Orden == "RECOMENDADOS_PRIMERO" && consulta.TamanioPagina == 12,
        "Recomendaciones no abre con su orden y tamaño propios");
    string html = System.Net.WebUtility.HtmlDecode(populares.ToHtmlString());
    Verificar(html.Contains("Recomendación popular") && html.Contains("25 libros")
        && !html.Contains("Buscar") && !html.Contains("MAS_VENDIDOS"), "Recomendaciones usa filtros o el catálogo general");
    await (Task)tipo.GetMethod("CambiarPagina", privados)!.Invoke(pantalla, [2])!;
    Verificar(ultimaConsultaRecomendaciones == id && ultimoOrdenRecomendaciones?.Pagina == 2
        && aperturasRecomendaciones == aperturas, "Paginar recalcula el algoritmo o cambia el conjunto");
    consulta.Orden = "PRECIO_ASCENDENTE";
    await (Task)tipo.GetMethod("Ordenar", privados)!.Invoke(pantalla, null)!;
    Verificar(ultimaConsultaRecomendaciones == id && ultimoOrdenRecomendaciones?.Pagina == 1
        && ultimoOrdenRecomendaciones.Orden == "PRECIO_ASCENDENTE" && aperturasRecomendaciones == aperturas,
        "Ordenar no reutiliza la consulta completa o no vuelve a la primera página");

    navegacion.Ir("/");
    historialSuficiente = false;
    var inicioSinHistorial = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Inicio>();
    string htmlInicio = System.Net.WebUtility.HtmlDecode(inicioSinHistorial.ToHtmlString());
    Verificar(htmlInicio.Contains("Más populares") && !htmlInicio.Contains(">Para ti</h2>")
        && ultimoOrdenRecomendaciones?.TamanioPagina == 8, "Inicio no limita populares a ocho u ofrece Para ti sin historial");

    navegacion.Ir("/para-ti");
    var sinHistorial = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Recomendaciones>();
    Verificar(sinHistorial.ToHtmlString().Contains("al menos tres pedidos"), "Para ti no explica la falta de historial");
    historialSuficiente = true;
    var personales = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Recomendaciones>();
    Verificar(System.Net.WebUtility.HtmlDecode(personales.ToHtmlString()).Contains("Recomendación personal")
        && ultimaRutaRecomendaciones?.EndsWith("/para-ti") == true, "Para ti no consulta las recomendaciones propias");

    navegacion.Ir("/");
    var inicioPersonal = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Inicio>();
    string htmlPersonal = System.Net.WebUtility.HtmlDecode(inicioPersonal.ToHtmlString());
    Verificar(htmlPersonal.Contains(">Para ti</h2>") && htmlPersonal.Contains("Recomendación personal")
        && ultimoOrdenRecomendaciones?.TamanioPagina == 8, "Inicio omite Para ti con historial suficiente o no limita a ocho");
    estado.LimpiarCuenta();
    Verificar(!System.Net.WebUtility.HtmlDecode(inicioPersonal.ToHtmlString()).Contains(">Para ti</h2>"),
        "Cerrar sesión conserva las recomendaciones personales de Inicio");
    navegacion.Ir("/para-ti");
    int antesDeVisitar = aperturasRecomendaciones;
    var visitante = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.Recomendaciones>();
    Verificar(System.Net.WebUtility.HtmlDecode(visitante.ToHtmlString()).Contains("Inicia sesión")
        && aperturasRecomendaciones == antesDeVisitar, "Para ti consulta recomendaciones privadas sin sesión");
    foreach (var pagina in componentes.Recomendaciones)
        await pagina.DisposeAsync();
    Verificar(consultasDescartadas.Contains(id), "Salir de recomendaciones no descarta su consulta temporal");
});
await renderer.Dispatcher.InvokeAsync(async () =>
{
    var privados = System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic;
    var redibujar = typeof(ComponentBase).GetMethod("StateHasChanged", privados)!;
    int accesosSolicitados = 0;
    string? aviso = null;
    estado.AccesoSolicitado += () => accesosSolicitados++;
    estado.AvisoSolicitado += mensaje => aviso = mensaje;
    var detalle = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Catalogo.DetalleLibro>(
        ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 7 }));
    var pantalla = componentes.Libro!;
    var tipo = pantalla.GetType();
    await (Task)tipo.GetMethod("EscribirResena", privados)!.Invoke(pantalla, null)!;
    Verificar(accesosSolicitados == 1 && comprobacionesPublicacion == 0,
        "Escribir una reseña sin sesión no solicita acceso o consulta datos privados");

    estado.MostrarPerfil(perfil, false);
    await (Task)tipo.GetMethod("EscribirResena", privados)!.Invoke(pantalla, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar(comprobacionesPublicacion == 1 && detalle.ToHtmlString().Contains("role=\"dialog\""),
        "Publicar no comprueba elegibilidad antes de abrir el formulario");
    var formulario = componentes.FormularioResena!;
    var tipoFormulario = formulario.GetType();
    var borrador = (DatosResenaDTO)tipoFormulario.GetField("borrador", privados)!.GetValue(formulario)!;
    borrador.Puntuacion = 4;
    borrador.Comentario = "Mi opinión";
    fallarGuardarResena = true;
    await (Task)tipoFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    redibujar.Invoke(formulario, null);
    Verificar(!resenaPublicada && detalle.ToHtmlString().Contains("Mi opini")
        && System.Net.WebUtility.HtmlDecode(detalle.ToHtmlString()).Contains("No se pudo guardar la reseña."),
        "El error al publicar pierde el borrador o no permite corregirlo");
    fallarGuardarResena = false;
    await (Task)tipoFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    var libro = (Web.Modelos.Libro)tipo.GetField("Libro", privados)!.GetValue(pantalla)!;
    Verificar(resenaPublicada && publicacionesResena == 1 && libro.Valoracion.CantidadResenas == 2
        && libro.Valoracion.Promedio == 4.5, "Publicar no actualiza la lista, el promedio y la distribución de estrellas");
    await (Task)tipo.GetMethod("EscribirResena", privados)!.Invoke(pantalla, null)!;
    Verificar(aviso == "Ya tienes una reseña publicada para este libro."
        && !(bool)tipo.GetField("formularioResena", privados)!.GetValue(pantalla)!
        && !System.Net.WebUtility.HtmlDecode(detalle.ToHtmlString()).Contains("Ver mi reseña"),
        "La reseña duplicada abre el formulario o conserva la acción retirada");

    var filas = (List<ResenaDTO>)tipo.GetField("resenas", privados)!.GetValue(pantalla)!;
    await (Task)tipo.GetMethod("AlternarVoto", privados)!.Invoke(pantalla, [filas.Single(r => r.Id == 1)])!;
    filas = (List<ResenaDTO>)tipo.GetField("resenas", privados)!.GetValue(pantalla)!;
    Verificar(filas.Single(r => r.Id == 1).VotoUtilPropio && filas.Single(r => r.Id == 1).CantidadVotosUtiles == 3,
        "Marcar útil no recupera el voto y el total vigentes");
    await (Task)tipo.GetMethod("AlternarVoto", privados)!.Invoke(pantalla, [filas.Single(r => r.Id == 1)])!;
    filas = (List<ResenaDTO>)tipo.GetField("resenas", privados)!.GetValue(pantalla)!;
    Verificar(!filas.Single(r => r.Id == 1).VotoUtilPropio && filas.Single(r => r.Id == 1).CantidadVotosUtiles == 2,
        "Quitar el voto útil no actualiza su estado y total");

    await (Task)tipo.GetMethod("ReportarResena", privados)!.Invoke(pantalla, [1])!;
    redibujar.Invoke(pantalla, null);
    var reporte = componentes.FormularioReporteResena!;
    var tipoReporte = reporte.GetType();
    var datosReporte = (DatosReporteResenaDTO)tipoReporte.GetField("borrador", privados)!.GetValue(reporte)!;
    datosReporte.Motivo = "PUBLICIDAD_O_SPAM";
    datosReporte.Comentario = "Contiene publicidad";
    await (Task)tipoReporte.GetMethod("Guardar", privados)!.Invoke(reporte, null)!;
    Verificar(enviosReporte == 1 && ultimoReporteResena?.Motivo == "PUBLICIDAD_O_SPAM"
        && ultimoReporteResena.Comentario == "Contiene publicidad"
        && tipo.GetField("resenaAReportar", privados)!.GetValue(pantalla) is null,
        "Enviar el reporte pierde el motivo/comentario o deja abierto el formulario");
    await (Task)tipo.GetMethod("ReportarResena", privados)!.Invoke(pantalla, [1])!;
    Verificar(aviso == "Ya tienes un reporte pendiente." && enviosReporte == 1
        && tipo.GetField("resenaAReportar", privados)!.GetValue(pantalla) is null,
        "Un reporte pendiente permite abrir otro formulario");

    var misResenas = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisResenas>();
    var propias = componentes.MisResenas!;
    var tipoPropias = propias.GetType();
    string html = System.Net.WebUtility.HtmlDecode(misResenas.ToHtmlString());
    Verificar(html.Contains("/portada-propia.png") && html.Contains("href=\"/libros/7\"")
        && html.Contains("16/11/2026") && !html.Contains("/foto-propia.png")
        && ultimaConsultaResenas?.TamanioPagina == 10,
        "Mis reseñas no muestra portada, enlace y fecha, muestra foto de autor o usa otro tamaño");
    var consulta = (ConsultaResenasDTO)tipoPropias.GetField("consulta", privados)!.GetValue(propias)!;
    consulta.Orden = "PUNTUACION_DESCENDENTE";
    await (Task)tipoPropias.GetMethod("Ordenar", privados)!.Invoke(propias, null)!;
    await (Task)tipoPropias.GetMethod("CambiarPagina", privados)!.Invoke(propias, [2])!;
    Verificar(ultimaConsultaResenas?.Orden == "PUNTUACION_DESCENDENTE" && ultimaConsultaResenas.Pagina == 2,
        "Mis reseñas pierde el orden al paginar");
    var resultadoPropias = (ResultadoPaginadoDTO<ResenaDTO>)tipoPropias.GetField("resultado", privados)!.GetValue(propias)!;
    tipoPropias.GetField("resenaEnEdicion", privados)!.SetValue(propias, resultadoPropias.Items.Single());
    redibujar.Invoke(propias, null);
    formulario = componentes.FormularioResena!;
    borrador = (DatosResenaDTO)tipoFormulario.GetField("borrador", privados)!.GetValue(formulario)!;
    Verificar(borrador.Puntuacion == 4 && borrador.Comentario == "Mi opinión", "Editar no precarga los datos del listado");
    borrador.Comentario = "Cambio cancelado";
    await formulario.AlCerrar.InvokeAsync();
    Verificar(contenidoResena.Comentario == "Mi opinión" && resultadoPropias.Items.Single().Comentario == "Mi opinión",
        "Cancelar una edición modifica la reseña guardada o su copia del listado");
    tipoPropias.GetField("resenaEnEdicion", privados)!.SetValue(propias, resultadoPropias.Items.Single());
    redibujar.Invoke(propias, null);
    formulario = componentes.FormularioResena!;
    borrador = (DatosResenaDTO)tipoFormulario.GetField("borrador", privados)!.GetValue(formulario)!;
    borrador.Puntuacion = 3;
    borrador.Comentario = "Opinión editada";
    await (Task)tipoFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    Verificar(contenidoResena.Puntuacion == 3 && contenidoResena.Comentario == "Opinión editada"
        && tipoPropias.GetField("resenaEnEdicion", privados)!.GetValue(propias) is null,
        "Guardar una edición no actualiza la reseña o deja abierto el formulario");
    await (Task)tipoPropias.GetMethod("Eliminar", privados)!.Invoke(propias, [2])!;
    redibujar.Invoke(propias, null);
    Verificar(!resenaPublicada && consulta.Pagina == 1
        && System.Net.WebUtility.HtmlDecode(misResenas.ToHtmlString()).Contains("No tienes reseñas publicadas."),
        "Eliminar deja la reseña visible o una página vacía posterior a la primera");
    await (Task)tipo.GetMethod("EscribirResena", privados)!.Invoke(pantalla, null)!;
    Verificar((bool)tipo.GetField("formularioResena", privados)!.GetValue(pantalla)! && publicacionesResena == 1,
        "Eliminar no permite abrir otra reseña o abrirla publica sin enviar");
});
await renderer.Dispatcher.InvokeAsync(async () =>
{
    var privados = System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic;
    var redibujar = typeof(ComponentBase).GetMethod("StateHasChanged", privados)!;
    estado.MostrarPerfil(perfil, false);
    var historial = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisPedidos>();
    var pantallaHistorial = componentes.MisPedidos!;
    var tipoHistorial = pantallaHistorial.GetType();
    string html = System.Net.WebUtility.HtmlDecode(historial.ToHtmlString());
    Verificar(ultimoFiltroPedido?.TamanioPagina == 10 && html.Contains("Pedido #21")
        && html.Contains("2 ejemplares") && html.Contains("+2 portadas") && !html.Contains("/portada5.png")
        && !html.Contains("Libro real") && !html.Contains("Autor real"),
        "El historial no muestra su composición limitada de portadas y datos, o añade títulos/autores");
    int consultasIniciales = consultasPedidos;
    tipoHistorial.GetMethod("AlternarEstado", privados)!.Invoke(pantallaHistorial, ["EN_ALMACEN"]);
    tipoHistorial.GetMethod("AlternarEstado", privados)!.Invoke(pantallaHistorial, ["ENTREGADO"]);
    Verificar(consultasPedidos == consultasIniciales, "Las casillas consultan el historial antes de Aplicar filtros");
    await (Task)tipoHistorial.GetMethod("Aplicar", privados)!.Invoke(pantallaHistorial, null)!;
    var filtro = (FiltroPedidoDTO)tipoHistorial.GetField("filtro", privados)!.GetValue(pantallaHistorial)!;
    Verificar(ultimoFiltroPedido!.Estados.SequenceEqual(["EN_ALMACEN", "ENTREGADO"])
        && ultimoFiltroPedido.Pagina == 1, "Aplicar no envía todos los estados seleccionados desde la primera página");
    filtro.Orden = "TOTAL_DESCENDENTE";
    await (Task)tipoHistorial.GetMethod("Ordenar", privados)!.Invoke(pantallaHistorial, null)!;
    await (Task)tipoHistorial.GetMethod("CambiarPagina", privados)!.Invoke(pantallaHistorial, [3])!;
    await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.MisPedidos>();
    Verificar(ultimoFiltroPedido?.Orden == "TOTAL_DESCENDENTE" && ultimoFiltroPedido.Pagina == 3
        && ultimoFiltroPedido.Estados.Count == 2, "Volver al historial pierde estados, orden o página");
    await (Task)tipoHistorial.GetMethod("Limpiar", privados)!.Invoke(pantallaHistorial, null)!;
    Verificar(ultimoFiltroPedido?.Estados.Count == 0 && ultimoFiltroPedido.Orden == "FECHA_PAGO_DESCENDENTE"
        && ultimoFiltroPedido.Pagina == 1, "Limpiar no retira los criterios y restablece la primera página");

    int lecturasIniciales = lecturasPedido;
    var detalle = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Compra.DetallePedido>(
        ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 21 }));
    var pantalla = componentes.Pedido!;
    var tipo = pantalla.GetType();
    string? aviso = null;
    estado.AvisoSolicitado += mensaje => aviso = mensaje;
    Verificar(lecturasPedido == lecturasIniciales + 1 && actualizacionesSeguimiento == 0
        && pedidoPrueba.Pedido.Estado == "EN_ALMACEN", "Entrar al detalle repite GET o avanza el seguimiento");
    await pantalla.SetParametersAsync(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 21 }));
    Verificar(lecturasPedido == lecturasIniciales + 1, "Repetir el mismo Id vuelve a consultar el pedido");
    await pantalla.SetParametersAsync(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 22 }));
    Verificar(lecturasPedido == lecturasIniciales + 2 && detalle.ToHtmlString().Contains("SIM-22"),
        "Cambiar Id en la misma instancia conserva el pedido anterior");
    await pantalla.SetParametersAsync(ParameterView.FromDictionary(new Dictionary<string, object?> { ["Id"] = 21 }));
    await (Task)tipo.GetMethod("ActualizarSeguimiento", privados)!.Invoke(pantalla, null)!;
    Verificar(aviso == "Aún no hay cambios en el seguimiento." && pedidoPrueba.Pedido.Estado == "EN_ALMACEN",
        "Actualizar antes del plazo no explica la ausencia de cambios");
    avanzarSeguimiento = true;
    await (Task)tipo.GetMethod("ActualizarSeguimiento", privados)!.Invoke(pantalla, null)!;
    Verificar(pedidoPrueba.Pedido.Estado == "EN_CAMINO" && aviso == "Pedido actualizado: En camino.",
        "Actualizar no recupera y anuncia la nueva etapa");
    await (Task)tipo.GetMethod("AbrirCancelacion", privados)!.Invoke(pantalla, null)!;
    Verificar(!(bool)tipo.GetField("formularioCancelacion", privados)!.GetValue(pantalla)!
        && aviso == "Solo se puede cancelar un pedido En almacén.",
        "Cancelar abre el formulario después de un preflight no elegible");
    pedidoPrueba = CrearPedido("EN_ALMACEN");
    await (Task)tipo.GetMethod("AbrirCancelacion", privados)!.Invoke(pantalla, null)!;
    var cancelacion = (DatosCancelacionDTO)tipo.GetField("cancelacion", privados)!.GetValue(pantalla)!;
    cancelacion.Motivo = "Ya no necesito el pedido";
    await (Task)tipo.GetMethod("CancelarPedido", privados)!.Invoke(pantalla, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar(pedidoPrueba.Pedido.Estado == "EN_ALMACEN"
        && (bool)tipo.GetField("formularioCancelacion", privados)!.GetValue(pantalla)!
        && detalle.ToHtmlString().Contains("Ya no necesito el pedido")
        && System.Net.WebUtility.HtmlDecode(detalle.ToHtmlString()).Contains("La cancelación simulada fue rechazada."),
        "El rechazo de cancelación pierde estado, formulario, motivo o mensaje");
    rechazarCancelacion = false;
    await (Task)tipo.GetMethod("CancelarPedido", privados)!.Invoke(pantalla, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar(pedidoPrueba.Pedido.Estado == "CANCELADO" && ultimaCancelacion?.Motivo == "Ya no necesito el pedido"
        && !(bool)tipo.GetField("formularioCancelacion", privados)!.GetValue(pantalla)!
        && aviso == "Pedido cancelado. Reembolso simulado: S/ 84.00."
        && !detalle.ToHtmlString().Contains("BS-21") && !detalle.ToHtmlString().Contains("tracking-panel")
        && !detalle.ToHtmlString().Contains("Reembolso"),
        "Cancelar no retira seguimiento o convierte el reembolso en aviso permanente del detalle");

    pedidoPrueba = CrearPedido("ENTREGADO");
    await (Task)tipo.GetMethod("CargarDatos", privados)!.Invoke(pantalla, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar(!detalle.ToHtmlString().Contains("tracking-steps") && !detalle.ToHtmlString().Contains("Actualizar seguimiento")
        && detalle.ToHtmlString().Contains("Fecha de entrega"), "Entregado mantiene etapas/actualización o pierde fechas");
    await (Task)tipo.GetMethod("AbrirDevolucion", privados)!.Invoke(pantalla, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar(permisosDevolucion == 1 && (bool)tipo.GetField("formularioDevolucion", privados)!.GetValue(pantalla)!,
        "Devolver no comprueba elegibilidad antes del formulario");
    var formulario = componentes.FormularioDevolucion!;
    var tipoFormulario = formulario.GetType();
    var borrador = (DatosDevolucionDTO)tipoFormulario.GetField("borrador", privados)!.GetValue(formulario)!;
    borrador.Motivo = "LIBRO_DANADO";
    borrador.Comentario = "La cubierta llegó rota";
    await (Task)tipoFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    Verificar(ultimaDevolucion?.Motivo == "LIBRO_DANADO" && ultimaDevolucion.Comentario == "La cubierta llegó rota"
        && pedidoPrueba.Pedido.Estado == "ENTREGADO"
        && !(bool)tipo.GetField("formularioDevolucion", privados)!.GetValue(pantalla)!
        && aviso == "Solicitud de devolución enviada. Está pendiente de revisión.",
        "Solicitar devolución pierde datos o anuncia aceptación antes de la moderación");
    await (Task)tipo.GetMethod("AbrirDevolucion", privados)!.Invoke(pantalla, null)!;
    Verificar(permisosDevolucion == 2 && aviso == "Ya hay una devolución pendiente para este pedido."
        && !(bool)tipo.GetField("formularioDevolucion", privados)!.GetValue(pantalla)!,
        "Una devolución pendiente permite abrir otro formulario");
    var consulta = await renderer.RenderComponentAsync<Web.Componentes.Compartidos.DetallePedidoContenido>(
        ParameterView.FromDictionary(new Dictionary<string, object?> { ["Pedido"] = pedidoPrueba }));
    string htmlConsulta = System.Net.WebUtility.HtmlDecode(consulta.ToHtmlString());
    Verificar(htmlConsulta.Contains("S/ 84.00") && htmlConsulta.Contains("****1234") && htmlConsulta.Contains("SIM-21")
        && !htmlConsulta.Contains("<button") && !htmlConsulta.Contains("Descargar comprobante"),
        "La referencia de pedido pierde importes/datos o incorpora acciones");
});
await renderer.Dispatcher.InvokeAsync(async () =>
{
    var privados = System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic;
    var redibujar = typeof(ComponentBase).GetMethod("StateHasChanged", privados)!;
    var navegacion = (NavegacionPrueba)proveedor.GetRequiredService<NavigationManager>();
    estado.MostrarPerfil(perfil, false);
    navegacion.Ir("/mis-solicitudes");
    var lista = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.Solicitudes>();
    var pantalla = componentes.Solicitudes!;
    var tipo = pantalla.GetType();
    async Task Accion(string nombre, params object[] argumentos)
        => await (Task)tipo.GetMethod(nombre, privados)!.Invoke(pantalla, argumentos)!;
    string Html() => System.Net.WebUtility.HtmlDecode(lista.ToHtmlString());
    void Cerrar()
    {
        tipo.GetMethod("CerrarDetalle", privados)!.Invoke(pantalla, null);
        redibujar.Invoke(pantalla, null);
    }
    Verificar(ultimoAmbitoSolicitud == "/webresources/solicitudes/propias/busqueda"
        && ultimoFiltroSolicitud?.Tipo == "DEVOLUCION" && ultimoFiltroSolicitud.TamanioPagina == 10,
        "Mis solicitudes no consulta el ámbito propio, pestaña inicial o tamaño de página");
    Verificar(Html().Contains("Solicitud #31") && Html().Contains("Editar") && Html().Contains("Retirar")
        && !Html().Contains("La cubierta llegó rota"), "Las tarjetas pierden acciones pendientes o despliegan comentarios");
    await Accion("CambiarEstado", "PENDIENTE");
    await Accion("CambiarEstado", "ACEPTADA");
    await Accion("CambiarPagina", 2);
    Verificar(ultimoFiltroSolicitud!.Estados.SequenceEqual(["PENDIENTE", "ACEPTADA"])
        && ultimoFiltroSolicitud.Pagina == 2, "Filtrar y paginar solicitudes pierde los estados múltiples");
    await Accion("MostrarTipo", "REPORTE_RESENA");
    redibujar.Invoke(pantalla, null);
    Verificar(ultimoFiltroSolicitud!.Tipo == "REPORTE_RESENA" && ultimoFiltroSolicitud.Pagina == 1
        && ultimoFiltroSolicitud.Estados.Count == 0 && Html().Contains("Cerrada"), "Cambiar pestaña no reinicia criterios ni habilita Cerrada en reportes");
    await Accion("Editar", solicitudReporte.Solicitud);
    redibujar.Invoke(pantalla, null);
    var formulario = componentes.FormularioReporteResena!;
    var claseFormulario = formulario.GetType();
    var borrador = (DatosReporteResenaDTO)claseFormulario.GetField("borrador", privados)!.GetValue(formulario)!;
    borrador.Comentario = "Comentario corregido";
    var detalle = (DetalleReporteResenaDTO)tipo.GetField("reporte", privados)!.GetValue(pantalla)!;
    Verificar(detalle.Datos.Comentario == "Comentario privado del reporte", "Editar modifica datos guardados antes de confirmar");
    fallarEdicionSolicitud = true;
    await (Task)claseFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    redibujar.Invoke(pantalla, null);
    Verificar((bool)tipo.GetField("editando", privados)!.GetValue(pantalla)!
        && borrador.Comentario == "Comentario corregido"
        && (string?)claseFormulario.GetField("error", privados)!.GetValue(formulario) == "No se pudo guardar el reporte.",
        "Un fallo de edición cierra el formulario, pierde entradas o esconde el error");
    fallarEdicionSolicitud = false;
    await (Task)claseFormulario.GetMethod("Guardar", privados)!.Invoke(formulario, null)!;
    Verificar(solicitudReporte.Datos.Comentario == "Comentario corregido"
        && solicitudReporte.ResenaReportada.ContenidoOriginal.Comentario == "Contenido original denunciado",
        "Guardar un reporte altera el contenido original denunciado");
    await Accion("Consultar", solicitudReporte.Solicitud);
    tipo.GetField("verResena", privados)!.SetValue(pantalla, true);
    redibujar.Invoke(pantalla, null);
    Verificar(Html().Contains("Contenido original denunciado") && !Html().Contains("Contenido actual editado")
        && !Html().Contains("Voto útil") && !Html().Contains("Guardar cambios"),
        "La referencia de reseña muestra contenido actualizado o acciones");
    Cerrar();
    await Accion("MostrarTipo", "DEVOLUCION");
    await Accion("Consultar", solicitudDevolucion.Solicitud);
    await Accion("VerPedido");
    redibujar.Invoke(pantalla, null);
    Verificar(Html().Contains("SIM-21") && Html().Contains("S/ 84.00")
        && !Html().Contains("Actualizar seguimiento") && !Html().Contains("Descargar comprobante")
        && !Html().Contains("Solicitar devolución"), "La referencia de pedido pierde importes o admite acciones");
    Cerrar();
    await Accion("Retirar", solicitudDevolucion.Solicitud);
    Verificar(devolucionRetirada && !Html().Contains("Solicitud #31"), "Retirar no elimina la solicitud del listado propio");

    int consultasAntes = consultasSolicitudes;
    navegacion.Ir("/moderacion");
    await pantalla.SetParametersAsync(ParameterView.Empty);
    redibujar.Invoke(pantalla, null);
    Verificar(consultasSolicitudes == consultasAntes && Html().Contains("Solo los administradores"),
        "Cambiar la misma pantalla a moderación permite consultar a un cliente");
    estado.LimpiarCuenta();
    var visitante = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.Solicitudes>();
    Verificar(consultasSolicitudes == consultasAntes && visitante.ToHtmlString().Contains("Iniciar sesión"),
        "Un visitante consulta moderación sin iniciar sesión");

    devolucionRetirada = false;
    estado.MostrarPerfil(perfil, true);
    lista = await renderer.RenderComponentAsync<Web.Componentes.Paginas.Cuenta.Solicitudes>();
    pantalla = componentes.Solicitudes!;
    Verificar(ultimoAmbitoSolicitud == "/webresources/solicitudes/moderacion/busqueda"
        && !Html().Contains(">Editar</button>") && !Html().Contains(">Retirar</button>"),
        "Moderación usa el ámbito propio o permite editar y retirar aportes ajenos");
    string? aviso = null;
    estado.AvisoSolicitado += mensaje => aviso = mensaje;
    await Accion("Consultar", solicitudDevolucion.Solicitud);
    redibujar.Invoke(pantalla, null);
    Verificar(Html().Contains("Solicitante: Ana") && Html().Contains("La cubierta llegó rota")
        && Html().Contains(">Aceptar</button>"), "La consulta administrativa no muestra solicitante, comentario o resolución");
    await Accion("Resolver", "ACEPTADA");
    redibujar.Invoke(pantalla, null);
    Verificar(solicitudDevolucion.Pedido.Estado == "DEVUELTO"
        && aviso == "Devolución aceptada. Reembolso simulado: S/ 84.00."
        && !Html().Contains("Reembolso"), "Resolver devolución no anuncia el reembolso temporal con envío o lo deja permanente");
    await Accion("MostrarTipo", "REPORTE_RESENA");
    await Accion("Consultar", solicitudReporte.Solicitud);
    redibujar.Invoke(pantalla, null);
    Verificar(Html().Contains("Contenido original denunciado") && Html().Contains("Contenido actual editado")
        && Html().Contains("Cerrar sin intervención"), "Moderación no compara originales/actuales o no admite cierre de contenido editado");
    await Accion("Resolver", "CERRADA");
    await Accion("Consultar", solicitudReporte.Solicitud);
    redibujar.Invoke(pantalla, null);
    Verificar(solicitudReporte.Solicitud.Estado == "CERRADA" && Html().Contains("Resuelta:")
        && !Html().Contains(">Aceptar</button>") && !Html().Contains(">Rechazar</button>"),
        "Una solicitud resuelta pierde su fecha o sigue ofreciendo resolución");
    navegacion.Ir("/mis-solicitudes");
    await pantalla.SetParametersAsync(ParameterView.Empty);
    Verificar(ultimoAmbitoSolicitud == "/webresources/solicitudes/propias/busqueda",
        "Volver desde moderación conserva el ámbito administrativo");
});
await servidor.StopAsync();
Console.WriteLine($"Correcto: {verificaciones} comprobaciones de contrato HTTP, JSON, cookies, compra y documentos.");

class NavegacionPrueba : NavigationManager
{
    public NavegacionPrueba() => Initialize("http://localhost/", "http://localhost/catalogo");
    public void Ir(string ruta) => Uri = ToAbsoluteUri(ruta).ToString();
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
    public Web.Componentes.Paginas.Compra.DetallePedido? Pedido { get; private set; }
    public Web.Componentes.Paginas.Administracion.Administracion? Administracion { get; private set; }
    public Web.Componentes.Paginas.Administracion.ModalReportes? Reportes { get; private set; }
    public Web.Componentes.Estructura.EstructuraPrincipal? Estructura { get; private set; }
    public Web.Componentes.Paginas.Cuenta.MisResenas? MisResenas { get; private set; }
    public Web.Componentes.Paginas.Cuenta.MisPedidos? MisPedidos { get; private set; }
    public Web.Componentes.Paginas.Cuenta.Solicitudes? Solicitudes { get; private set; }
    public Web.Componentes.Compartidos.FormularioResena? FormularioResena { get; private set; }
    public Web.Componentes.Compartidos.FormularioReporteResena? FormularioReporteResena { get; private set; }
    public Web.Componentes.Compartidos.FormularioDevolucion? FormularioDevolucion { get; private set; }
    public List<Web.Componentes.Paginas.Catalogo.Recomendaciones> Recomendaciones { get; } = [];
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
        if (componente is Web.Componentes.Paginas.Compra.DetallePedido pedido)
            Pedido = pedido;
        if (componente is Web.Componentes.Paginas.Administracion.Administracion administracion)
            Administracion = administracion;
        if (componente is Web.Componentes.Paginas.Administracion.ModalReportes reportes)
            Reportes = reportes;
        if (componente is Web.Componentes.Estructura.EstructuraPrincipal estructura)
            Estructura = estructura;
        if (componente is Web.Componentes.Paginas.Cuenta.MisResenas misResenas)
            MisResenas = misResenas;
        if (componente is Web.Componentes.Paginas.Cuenta.MisPedidos misPedidos)
            MisPedidos = misPedidos;
        if (componente is Web.Componentes.Paginas.Cuenta.Solicitudes solicitudes)
            Solicitudes = solicitudes;
        if (componente is Web.Componentes.Compartidos.FormularioResena formularioResena)
            FormularioResena = formularioResena;
        if (componente is Web.Componentes.Compartidos.FormularioReporteResena formularioReporteResena)
            FormularioReporteResena = formularioReporteResena;
        if (componente is Web.Componentes.Compartidos.FormularioDevolucion formularioDevolucion)
            FormularioDevolucion = formularioDevolucion;
        if (componente is Web.Componentes.Paginas.Catalogo.Recomendaciones recomendaciones)
            Recomendaciones.Add(recomendaciones);
        return componente;
    }
}

class NavegadorPdfPrueba : IJSRuntime
{
    public string? NombreArchivo { get; private set; }
    public byte[] Contenido { get; private set; } = [];

    public ValueTask<TValue> InvokeAsync<TValue>(string identifier, object?[]? args)
    {
        if (args?.LastOrDefault() is DotNetStreamReference referencia)
        {
            using var copia = new MemoryStream();
            referencia.Stream.CopyTo(copia);
            Contenido = copia.ToArray();
        }
        if (identifier == "bookshopDescargar")
            NombreArchivo = (string)args![0]!;
        return ValueTask.FromResult(default(TValue)!);
    }

    public ValueTask<TValue> InvokeAsync<TValue>(string identifier, CancellationToken cancellationToken, object?[]? args)
        => InvokeAsync<TValue>(identifier, args);
}
