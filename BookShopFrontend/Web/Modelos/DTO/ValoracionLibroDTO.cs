using Newtonsoft.Json;

namespace Web.Modelos.DTO;

public class ValoracionLibroDTO
{
    [JsonProperty("idLibro")]
    public int IdLibro { get; set; }

    [JsonProperty("cantidadUnaEstrella")]
    public long CantidadUnaEstrella { get; set; }

    [JsonProperty("cantidadDosEstrellas")]
    public long CantidadDosEstrellas { get; set; }

    [JsonProperty("cantidadTresEstrellas")]
    public long CantidadTresEstrellas { get; set; }

    [JsonProperty("cantidadCuatroEstrellas")]
    public long CantidadCuatroEstrellas { get; set; }

    [JsonProperty("cantidadCincoEstrellas")]
    public long CantidadCincoEstrellas { get; set; }

    public long CantidadResenas => CantidadUnaEstrella + CantidadDosEstrellas + CantidadTresEstrellas
        + CantidadCuatroEstrellas + CantidadCincoEstrellas;

    public double Promedio
    {
        get
        {
            if (CantidadResenas == 0)
                return 0;
            long puntos = CantidadUnaEstrella + 2 * CantidadDosEstrellas + 3 * CantidadTresEstrellas
                + 4 * CantidadCuatroEstrellas + 5 * CantidadCincoEstrellas;
            return puntos / (double)CantidadResenas;
        }
    }

    public ValoracionLibroDTO()
    {
    }

    public ValoracionLibroDTO(
        int idLibro, long cantidadUnaEstrella, long cantidadDosEstrellas, long cantidadTresEstrellas,
        long cantidadCuatroEstrellas, long cantidadCincoEstrellas)
    {
        IdLibro = idLibro;
        CantidadUnaEstrella = cantidadUnaEstrella;
        CantidadDosEstrellas = cantidadDosEstrellas;
        CantidadTresEstrellas = cantidadTresEstrellas;
        CantidadCuatroEstrellas = cantidadCuatroEstrellas;
        CantidadCincoEstrellas = cantidadCincoEstrellas;
    }
}
