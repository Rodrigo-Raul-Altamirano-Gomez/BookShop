using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Serialization;

namespace BookShopWeb.Code;

// Llamadas al API REST del backend. La ruta es relativa a la URL del API (Api.Url).
public class HttpClientUtils<T>
{
    private static readonly HttpClient http = new HttpClient();
    private static readonly JsonSerializerSettings opciones = new JsonSerializerSettings
    {
        ContractResolver = new CamelCasePropertyNamesContractResolver(),
        Converters = { new StringEnumConverter() },
        NullValueHandling = NullValueHandling.Ignore
    };

    public async Task<T?> Get(string ruta)
    {
        return await Leer(await http.GetAsync(Api.Url + ruta));
    }

    public async Task<T?> Post(string ruta, object datos)
    {
        return await Leer(await http.PostAsync(Api.Url + ruta, Cuerpo(datos)));
    }

    public async Task<T?> Put(string ruta, object datos)
    {
        return await Leer(await http.PutAsync(Api.Url + ruta, Cuerpo(datos)));
    }

    public async Task<T?> Delete(string ruta)
    {
        return await Leer(await http.DeleteAsync(Api.Url + ruta));
    }

    // Un archivo (por ejemplo, un PDF) tal como lo envía el API
    public async Task<byte[]> GetArchivo(string ruta)
    {
        HttpResponseMessage respuesta = await http.GetAsync(Api.Url + ruta);
        if (!respuesta.IsSuccessStatusCode)
        {
            throw new ApiException((int)respuesta.StatusCode, await respuesta.Content.ReadAsStringAsync());
        }
        return await respuesta.Content.ReadAsByteArrayAsync();
    }

    private StringContent Cuerpo(object datos)
    {
        string json = JsonConvert.SerializeObject(datos, opciones);
        return new StringContent(json, Encoding.UTF8, "application/json");
    }

    private async Task<T?> Leer(HttpResponseMessage respuesta)
    {
        string texto = await respuesta.Content.ReadAsStringAsync();
        if (!respuesta.IsSuccessStatusCode)
        {
            // El backend envía el mensaje de la excepción de negocio como texto
            throw new ApiException((int)respuesta.StatusCode, texto);
        }
        if (texto == "") return default;
        return JsonConvert.DeserializeObject<T>(texto, opciones);
    }
}

// Error que devolvió el API: el código HTTP y el mensaje para el usuario
public class ApiException : Exception
{
    public int Estado { get; }

    public ApiException(int estado, string mensaje) : base(mensaje)
    {
        Estado = estado;
    }
}

public static class Api
{
    // Se lee de appsettings.json ("ApiUrl") o de la variable de entorno ApiUrl
    public static string Url { get; set; } = "";
}
