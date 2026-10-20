using System.Globalization;
using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisResenas
{
    private const string ClaveVista = "cuenta:mis-resenas";
    private static readonly CultureInfo Espanol = CultureInfo.GetCultureInfo("es-PE");
    private string Orden { get; set; } = "recent";
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 10;
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Resenas.Count / (double)TamanoPagina));
    private Resena? EnEdicion
    {
        get; set;
    }
    private int Puntuacion
    {
        get; set;
    }
    private string Comentario { get; set; } = "";

    private List<Resena> Resenas
    {
        get
        {
            if (Estado.Usuario is null)
                return [];
            var resenas = Estado.Resenas.Where(resena => resena.Propia && resena.Activo);
            return (Orden switch
            {
                "votes-asc" => resenas.OrderBy(resena => resena.Votos),
                "votes-desc" => resenas.OrderByDescending(resena => resena.Votos),
                "stars-asc" => resenas.OrderBy(resena => resena.Puntuacion),
                "stars-desc" => resenas.OrderByDescending(resena => resena.Puntuacion),
                _ => resenas.OrderByDescending(resena => resena.Fecha)
            }).ToList();
        }
    }

    private IEnumerable<Resena> ResenasPagina => Resenas.Skip((Pagina - 1) * TamanoPagina).Take(TamanoPagina);

    private static string Estrellas(int cantidad) => new('★', cantidad);
    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Estado.EstadoVistas.TryGetValue(ClaveVista, out var guardado) && guardado is VistaResenas vista)
        {
            Orden = vista.Orden;
            Pagina = Estado.Usuario is null ? vista.Pagina : Math.Min(vista.Pagina, TotalPaginas);
        }
    }

    private void GuardarVista() => Estado.EstadoVistas[ClaveVista] = new VistaResenas
    {

        Orden = Orden,

        Pagina = Pagina

    };
    private void ReiniciarPagina()
    {
        Pagina = 1;
        GuardarVista();
    }

    private void CambiarPagina(int pagina)
    {
        Pagina = pagina;
        GuardarVista();
    }

    private void SeleccionarPuntuacion(int puntuacion) => Puntuacion = puntuacion;
    private void Editar(Resena resena)
    {
        EnEdicion = resena;
        Puntuacion = resena.Puntuacion;
        Comentario = resena.Comentario;
    }

    private void Cerrar() => EnEdicion = null;
    private void Guardar()
    {
        if (EnEdicion is null || Puntuacion == 0 || string.IsNullOrWhiteSpace(Comentario))
            return;
        EnEdicion.Puntuacion = Puntuacion;
        EnEdicion.Comentario = Comentario.Trim();
        Cerrar();
        Estado.Notificar();
        Estado.Avisar("Reseña actualizada.");
    }

    private void Eliminar(Resena resena)
    {
        resena.Activo = false;
        Pagina = Math.Min(Pagina, TotalPaginas);
        GuardarVista();
        Estado.Notificar();
        Estado.Avisar("Reseña eliminada.");
    }

    private class VistaResenas
    {
        public string Orden { get; set; } = "recent";
        public int Pagina { get; set; } = 1;
    }
}

