using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ResenaDTO
{
    [JsonProperty("id")]
    public int Id
    {
        get; set;
    }

    [JsonProperty("idLibro")]
    public int IdLibro
    {
        get; set;
    }

    [JsonProperty("tituloLibro")]
    public string TituloLibro { get; set; } = "";

    [JsonProperty("portadaLibro")]
    public ImagenDTO? PortadaLibro
    {
        get; set;
    }

    [JsonProperty("nombreUsuario")]
    public string NombreUsuario { get; set; } = "";

    [JsonProperty("imagenUsuario")]
    public ImagenDTO? ImagenUsuario
    {
        get; set;
    }

    [JsonProperty("puntuacion")]
    public int Puntuacion
    {
        get; set;
    }

    [JsonProperty("comentario")]
    public string Comentario { get; set; } = "";

    [JsonProperty("fecha")]
    public DateTime Fecha
    {
        get; set;
    }

    [JsonProperty("cantidadVotosUtiles")]
    public int CantidadVotosUtiles
    {
        get; set;
    }

    [JsonProperty("votoUtilPropio")]
    public bool VotoUtilPropio
    {
        get; set;
    }

    [JsonProperty("activo")]
    public bool Activo
    {
        get; set;
    }

    public ResenaDTO()
    {
    }

    public ResenaDTO(int id, int idLibro, string tituloLibro, ImagenDTO? portadaLibro, string nombreUsuario, ImagenDTO? imagenUsuario, int puntuacion, string comentario, DateTime fecha, int cantidadVotosUtiles, bool votoUtilPropio, bool activo)
    {
        Id = id;
        IdLibro = idLibro;
        TituloLibro = tituloLibro;
        PortadaLibro = portadaLibro;
        NombreUsuario = nombreUsuario;
        ImagenUsuario = imagenUsuario;
        Puntuacion = puntuacion;
        Comentario = comentario;
        Fecha = fecha;
        CantidadVotosUtiles = cantidadVotosUtiles;
        VotoUtilPropio = votoUtilPropio;
        Activo = activo;
    }
}
