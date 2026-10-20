using System.Globalization;
using Web.Modelos;

namespace Web.Demostracion;
// Estado del circuito Blazor: cada pestaña comienza con sus propios ejemplos.
// Los cambios sirven exclusivamente para demostrar la interacción de la interfaz.
public class EstadoDemostracion
{
    public List<Libro> Libros { get; } = DatosDemostracion.CrearLibros();
    public List<Referencia> Referencias { get; } = DatosDemostracion.CrearReferencias();
    public List<Resena> Resenas { get; } = DatosDemostracion.CrearResenas();
    public List<Direccion> Direcciones { get; } = DatosDemostracion.CrearDirecciones();
    public List<MetodoPago> Metodos { get; } = DatosDemostracion.CrearMetodos();
    public List<Cupon> Cupones { get; } = DatosDemostracion.CrearCupones();
    public List<Pedido> Pedidos
    {
        get;
    }
    public List<Solicitud> Solicitudes { get; } = DatosDemostracion.CrearSolicitudes();
    public Perfil? Usuario
    {
        get; set;
    }
    public Dictionary<int, int> Carrito { get; } = [];
    public HashSet<int> Deseados { get; } = [1, 4, 8, 10];
    public HashSet<int> Favoritos { get; } = [2, 3, 6];
    public HashSet<string> Seguimientos { get; } = ["autores:1", "categorias:2", "editoriales:1"];
    public Dictionary<string, int> OrdenSeguimientos
    {
        get;
    } = new()
    {

        ["autores:1"] = 1,

        ["categorias:2"] = 2,

        ["editoriales:1"] = 3

    };
    public int? IdComparacion
    {
        get; set;
    }
    public Dictionary<string, object> EstadoVistas { get; } = [];
    public int CantidadCarrito => Carrito.Values.Sum();
    public bool EsAdministrador => Usuario?.EsAdministrador == true;

    public event Action? Cambiado;
    public event Action? AccesoSolicitado;
    public event Action<string>? AvisoSolicitado;
    public EstadoDemostracion() => Pedidos = DatosDemostracion.CrearPedidos(Direcciones[0], Metodos[0]);
    public Libro? BuscarLibro(int id) => Libros.FirstOrDefault(b => b.Id == id);
    public void Notificar() => Cambiado?.Invoke();
    public void Avisar(string message) => AvisoSolicitado?.Invoke(message);
    public bool RequerirAcceso()
    {
        if (Usuario is not null)
            return true;
        AccesoSolicitado?.Invoke();
        return false;
    }

    public void MostrarAcceso() => AccesoSolicitado?.Invoke();
    public static string Dinero(decimal value) => "S/ " + value.ToString("N2", CultureInfo.GetCultureInfo(
        "es-PE"));
}

