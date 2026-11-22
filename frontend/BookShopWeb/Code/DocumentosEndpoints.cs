namespace BookShopWeb.Code;

// Documentos PDF: el navegador los abre en una pestaña nueva con un enlace normal (target="_blank");
// el front los pide al API y los entrega tal cual
public static class DocumentosEndpoints
{
    public static IEndpointRouteBuilder MapDocumentosEndpoints(this IEndpointRouteBuilder endpoints)
    {
        // RF17: comprobante de un pedido
        endpoints.MapGet("/comprobantes/{id:int}", async (int id, HttpContext context) =>
        {
            byte[] pdf = await new HttpClientUtils<object>().GetArchivo($"pedidos/{id}/comprobante");
            context.Response.Headers.ContentDisposition = $"inline; filename=\"comprobante-{id}.pdf\"";
            return Results.File(pdf, "application/pdf");
        }).RequireAuthorization();

        return endpoints;
    }
}
