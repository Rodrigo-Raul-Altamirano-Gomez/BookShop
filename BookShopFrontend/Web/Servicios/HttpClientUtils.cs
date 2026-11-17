using System.Net;
using System.Text;
using Newtonsoft.Json;

namespace Web.Servicios;

// Cliente REST del backend. Hay una instancia por usuario conectado (servicio Scoped),
// así cada uno conserva su propia cookie de sesión.
public class HttpClientUtils : IDisposable
{
    private readonly HttpClient cliente;
    private readonly CookieContainer cookies = new CookieContainer();

    // Se avisa cuando el backend responde 401: la sesión ya no existe.
    public event Action? SesionVencida;

    public HttpClientUtils(IConfiguration configuracion)
    {
        HttpClientHandler manejador = new HttpClientHandler();
        manejador.CookieContainer = cookies;
        cliente = new HttpClient(manejador);
        cliente.Timeout = TimeSpan.FromSeconds(30);
        cliente.BaseAddress = new Uri(configuracion["Backend:Url"]!);
    }

    public async Task<T> Get<T>(string ruta)
    {
        string json = await Enviar(HttpMethod.Get, ruta, null);
        return JsonConvert.DeserializeObject<T>(json)!;
    }

    public async Task<T> Post<T>(string ruta, object? datos = null)
    {
        string json = await Enviar(HttpMethod.Post, ruta, datos);
        return JsonConvert.DeserializeObject<T>(json)!;
    }

    public async Task<T> Put<T>(string ruta, object? datos = null)
    {
        string json = await Enviar(HttpMethod.Put, ruta, datos);
        return JsonConvert.DeserializeObject<T>(json)!;
    }

    public async Task<T> Delete<T>(string ruta)
    {
        string json = await Enviar(HttpMethod.Delete, ruta, null);
        return JsonConvert.DeserializeObject<T>(json)!;
    }

    public async Task Post(string ruta, object? datos = null)
    {
        await Enviar(HttpMethod.Post, ruta, datos);
    }

    public async Task Put(string ruta, object? datos = null)
    {
        await Enviar(HttpMethod.Put, ruta, datos);
    }

    public async Task Delete(string ruta)
    {
        await Enviar(HttpMethod.Delete, ruta, null);
    }

    // Descarga un PDF (comprobante o reporte).
    public async Task<byte[]> GetArchivo(string ruta)
    {
        HttpRequestMessage peticion = new HttpRequestMessage(HttpMethod.Get, ruta);
        HttpResponseMessage respuesta = await Ejecutar(peticion);
        return await respuesta.Content.ReadAsByteArrayAsync();
    }

    // Envía la petición con el cuerpo en JSON y devuelve el JSON de la respuesta.
    private async Task<string> Enviar(HttpMethod metodo, string ruta, object? datos)
    {
        HttpRequestMessage peticion = new HttpRequestMessage(metodo, ruta);
        if (datos != null || metodo == HttpMethod.Put)
        {
            string cuerpo = JsonConvert.SerializeObject(datos);
            peticion.Content = new StringContent(cuerpo, Encoding.UTF8, "application/json");
        }
        HttpResponseMessage respuesta = await Ejecutar(peticion);
        return await respuesta.Content.ReadAsStringAsync();
    }

    // Si el backend responde con error, lanza InvalidOperationException con su mensaje.
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
            throw new InvalidOperationException("El backend tardó demasiado en responder.");
        }
        if (respuesta.IsSuccessStatusCode)
            return respuesta;

        if (respuesta.StatusCode == HttpStatusCode.Unauthorized)
            SesionVencida?.Invoke();
        string mensaje = "No se pudo completar la operación.";
        string texto = await respuesta.Content.ReadAsStringAsync();
        if (respuesta.Content.Headers.ContentType?.MediaType == "application/json")
        {
            ErrorRestDTO? error = JsonConvert.DeserializeObject<ErrorRestDTO>(texto);
            if (error != null && error.Mensaje != "")
                mensaje = error.Mensaje;
        }
        throw new InvalidOperationException(mensaje);
    }

    public void OlvidarSesion()
    {
        foreach (Cookie cookie in cookies.GetAllCookies())
            cookie.Expired = true;
    }

    public void Dispose()
    {
        cliente.Dispose();
    }
}
