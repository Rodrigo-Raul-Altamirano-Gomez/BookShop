using Web.Modelos;
using Web.Modelos.DTO;

namespace Web.Servicios;

public class SesionUsuario
{
    private readonly HttpClientUtils api;
    public Perfil? Usuario { get; set; }
    public bool EsAdministrador => Usuario?.EsAdministrador == true;
    public int? IdComparacion { get; set; }
    public Dictionary<string, object> EstadoVistas { get; } = [];
    public string? Error { get; set; }
    public event Action? Cambiado;
    public event Action? Cerrada;
    public event Action? AccesoSolicitado;
    public event Action<string>? AvisoSolicitado;

    public SesionUsuario(HttpClientUtils api)
    {
        this.api = api;
        api.SesionVencida += LimpiarCuenta;
    }

    public void LimpiarCuenta()
    {
        api.OlvidarSesion();
        Usuario = null;
        EstadoVistas.Clear();
        IdComparacion = null;
        Error = null;
        Cerrada?.Invoke();
        Notificar();
    }

    public void MostrarPerfil(PerfilUsuarioDTO dto, bool? administrador = null)
    {
        Usuario = new Perfil
        {
            Nombre = dto.Usuario.Nombre,
            Correo = dto.Usuario.Email,
            Nacimiento = dto.FechaNacimiento,
            Imagen = Presentacion.Imagen(dto.Usuario.Imagen, "/images/user-placeholder.svg"),
            EsAdministrador = administrador ?? EsAdministrador
        };
    }

    public void Notificar() => Cambiado?.Invoke();

    public void Avisar(string mensaje) => AvisoSolicitado?.Invoke(mensaje);

    public bool RequerirAcceso()
    {
        if (Usuario is not null)
            return true;
        MostrarAcceso();
        return false;
    }

    public void MostrarAcceso() => AccesoSolicitado?.Invoke();

}
