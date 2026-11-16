using Microsoft.JSInterop;

namespace Web.Servicios;

// RF17, RF18 y RF19: pide el PDF al backend con la sesión del circuito y lo entrega al navegador.
// La cookie de sesión vive en el servidor Blazor, por eso el navegador no descarga directamente del backend.
public class DocumentoServicio
{
    private readonly HttpClientUtils api;
    private readonly IJSRuntime js;

    public DocumentoServicio(HttpClientUtils api, IJSRuntime js)
    {
        this.api = api;
        this.js = js;
    }

    public Task DescargarComprobante(int idPedido) =>
        Descargar($"documentos/comprobantes/{idPedido}", $"comprobante-pedido-{idPedido}.pdf");

    public Task DescargarVentasLibros(DateOnly inicio, DateOnly fin, int? cantidad) =>
        Descargar($"documentos/ventas-libros?inicio={inicio:yyyy-MM-dd}&fin={fin:yyyy-MM-dd}{Cantidad(cantidad)}",
            $"reporte-ventas-libros-{inicio:yyyy-MM-dd}-{fin:yyyy-MM-dd}.pdf");

    public Task DescargarUsuarios(int anio, int mes, int? cantidad) =>
        Descargar($"documentos/usuarios?mes={anio:D4}-{mes:D2}{Cantidad(cantidad)}",
            $"reporte-usuarios-{anio:D4}-{mes:D2}.pdf");

    // Genera el comprobante en el backend y devuelve una URL local para mostrarlo dentro de la página.
    public async Task<string> UrlComprobante(int idPedido)
    {
        var contenido = await api.GetArchivo($"documentos/comprobantes/{idPedido}");
        using var flujo = new MemoryStream(contenido);
        using var referencia = new DotNetStreamReference(flujo);
        return await js.InvokeAsync<string>("bookshopUrlPdf", referencia);
    }

    public ValueTask LiberarUrl(string url) => js.InvokeVoidAsync("bookshopLiberarUrl", url);

    private static string Cantidad(int? cantidad) => cantidad is null ? "" : $"&cantidad={cantidad}";

    private async Task Descargar(string ruta, string nombreArchivo)
    {
        var contenido = await api.GetArchivo(ruta);
        using var flujo = new MemoryStream(contenido);
        using var referencia = new DotNetStreamReference(flujo);
        await js.InvokeVoidAsync("bookshopDescargar", nombreArchivo, referencia);
    }
}
