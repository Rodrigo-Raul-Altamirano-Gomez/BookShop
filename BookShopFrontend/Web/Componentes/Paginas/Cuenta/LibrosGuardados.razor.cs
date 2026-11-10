using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class LibrosGuardados
{
    private const string ClavePestana = "libros-guardados:pestana-favoritos";
    private bool PestanaFavoritos { get; set; }

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue(ClavePestana, out var pestana) && pestana is bool favoritos)
            PestanaFavoritos = favoritos;
    }

    private void MostrarDeseados()
    {
        PestanaFavoritos = false;
        Sesion.EstadoVistas[ClavePestana] = false;
    }

    private void MostrarFavoritos()
    {
        PestanaFavoritos = true;
        Sesion.EstadoVistas[ClavePestana] = true;
    }
}
