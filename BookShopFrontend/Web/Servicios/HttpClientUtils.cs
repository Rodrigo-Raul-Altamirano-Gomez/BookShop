using System.Net;
using Newtonsoft.Json;
using System.Text;

namespace Web.Servicios;

// Una instancia por circuito Blazor: las cookies de dos usuarios nunca se comparten.
public sealed class HttpClientUtils : IDisposable
{
    private readonly HttpClient cliente;
    private readonly CookieContainer cookies = new();

    public event Action? SesionVencida;

    public HttpClientUtils(IConfiguration configuracion)
    {
        cliente = new HttpClient(new HttpClientHandler { CookieContainer = cookies });
        cliente.Timeout = TimeSpan.FromSeconds(30);
        cliente.DefaultRequestHeaders.Accept.ParseAdd("application/json");
        cliente.BaseAddress = new Uri(configuracion["Backend:Url"]!.TrimEnd('/') + "/");
        cliente.DefaultRequestHeaders.Add("X-BookShop-Cliente", "web");
    }

    public Task<T> Get<T>(string ruta) => Enviar<T>(HttpMethod.Get, ruta);
    public Task<T> Post<T>(string ruta, object? datos = null) => Enviar<T>(HttpMethod.Post, ruta, datos);
    public Task<T> Put<T>(string ruta, object? datos = null) => Enviar<T>(HttpMethod.Put, ruta, datos);
    public Task<T> Delete<T>(string ruta) => Enviar<T>(HttpMethod.Delete, ruta);
    public Task Post(string ruta, object? datos = null) => Enviar<object>(HttpMethod.Post, ruta, datos);
    public Task Put(string ruta, object? datos = null) => Enviar<object>(HttpMethod.Put, ruta, datos);
    public Task Delete(string ruta) => Enviar<object>(HttpMethod.Delete, ruta);

    // Descarga binaria (PDF de RF17, RF18 y RF19). Los errores llegan como JSON igual que en las demás llamadas.
    public async Task<byte[]> GetArchivo(string ruta, string tipo = "application/pdf")
    {
        using var peticion = new HttpRequestMessage(HttpMethod.Get, ruta);
        peticion.Headers.Accept.Clear();
        peticion.Headers.Accept.ParseAdd(tipo);
        peticion.Headers.Accept.ParseAdd("application/json");
        using var respuesta = await Ejecutar(peticion);
        return await respuesta.Content.ReadAsByteArrayAsync();
    }

    private async Task<T> Enviar<T>(HttpMethod metodo, string ruta, object? datos = null)
    {
        using var peticion = new HttpRequestMessage(metodo, ruta);
        if (datos is not null || metodo == HttpMethod.Put)
            peticion.Content = new StringContent(JsonConvert.SerializeObject(datos), Encoding.UTF8, "application/json");
        using var respuesta = await Ejecutar(peticion);
        if (respuesta.StatusCode == HttpStatusCode.NoContent)
            return default!;
        var contenido = await respuesta.Content.ReadAsStringAsync();
        return JsonConvert.DeserializeObject<T>(contenido)!;
    }

    // Envía la petición y convierte las respuestas de error en InvalidOperationException con el mensaje del backend.
    private async Task<HttpResponseMessage> Ejecutar(HttpRequestMessage peticion)
    {
        HttpResponseMessage respuesta;
        try
        {
            respuesta = await cliente.SendAsync(peticion);
        }
        catch (HttpRequestException)
        {
            throw new InvalidOperationException("No se pudo conectar con el backend.");
        }
        catch (TaskCanceledException)
        {
            throw new InvalidOperationException("El backend tardó demasiado en responder. Consulta el estado antes de repetir la operación.");
        }
        if (respuesta.IsSuccessStatusCode)
            return respuesta;
        using (respuesta)
        {
            if (respuesta.StatusCode == HttpStatusCode.Unauthorized)
                SesionVencida?.Invoke();
            var texto = await respuesta.Content.ReadAsStringAsync();
            string mensaje = "No se pudo completar la operación.";
            if (respuesta.Content.Headers.ContentType?.MediaType == "application/json")
            {
                mensaje = JsonConvert.DeserializeObject<ErrorRestDTO>(texto)?.Mensaje ?? mensaje;
            }
            throw new InvalidOperationException(mensaje);
        }
    }

    public void OlvidarSesion()
    {
        foreach (Cookie cookie in cookies.GetAllCookies())
            cookie.Expired = true;
    }

    public void Dispose() => cliente.Dispose();
}
