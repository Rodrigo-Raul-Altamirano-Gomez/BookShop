using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class LibrosGuardados
{
    private const string ClavePestana = "libros-guardados:pestana-favoritos";
    private bool PestanaFavoritos
    {
        get; set;
    }
    private IEnumerable<Libro> Libros => Estado.Libros.Where(libro => (PestanaFavoritos ? Estado.Favoritos
        : Estado.Deseados).Contains(libro.Id));

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Estado.EstadoVistas.TryGetValue(ClavePestana, out var pestana) && pestana is bool favoritos)
            PestanaFavoritos = favoritos;
    }

    private void MostrarDeseados()
    {
        PestanaFavoritos = false;
        Estado.EstadoVistas[ClavePestana] = false;
    }

    private void MostrarFavoritos()
    {
        PestanaFavoritos = true;
        Estado.EstadoVistas[ClavePestana] = true;
    }
}

