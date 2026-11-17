using Web.Modelos;

namespace Web.Servicios;

public class SesionUsuario
{
    // El encabezado delega la búsqueda en el listado visible.
    public Func<string, Task>? BuscarEnListado { get; set; }
    public string TextoBusqueda { get; set; } = "";
    public string IndicacionBusqueda { get; set; } = "Buscar por título";
    public event Action? BusquedaCambiada;
    private readonly HttpClientUtils api;
    public Perfil? Usuario { get; set; }
    public bool EsAdministrador => Usuario?.EsAdministrador == true;
    public Libro? LibroComparacion { get; set; }
    public Dictionary<string, object> EstadoVistas { get; } = [];
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
        LibroComparacion = null;
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
            Imagen = dto.Usuario.Imagen ?? "/images/user-placeholder.svg",
            EsAdministrador = administrador ?? EsAdministrador
        };
    }

    public void Notificar() => Cambiado?.Invoke();

    public void MostrarBusqueda(string texto, string indicacion, Func<string, Task> buscar)
    {
        BuscarEnListado = buscar;
        if (TextoBusqueda == texto && IndicacionBusqueda == indicacion)
            return;
        TextoBusqueda = texto;
        IndicacionBusqueda = indicacion;
        BusquedaCambiada?.Invoke();
    }

    public void Avisar(string mensaje) => AvisoSolicitado?.Invoke(mensaje);

    public bool RequerirAcceso()
    {
        if (Usuario != null)
            return true;
        MostrarAcceso();
        return false;
    }

    public void MostrarAcceso() => AccesoSolicitado?.Invoke();

}
