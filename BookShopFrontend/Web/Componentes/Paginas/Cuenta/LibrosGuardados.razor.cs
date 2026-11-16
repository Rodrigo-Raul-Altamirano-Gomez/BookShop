using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class LibrosGuardados
{
    private const string ClavePestana = "libros-guardados:pestana-favoritos";
    private string TextoBusqueda = "", Busqueda = "";
    private void Buscar() => Busqueda = TextoBusqueda.Trim();
    private bool PestanaFavoritos { get; set; }

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue(ClavePestana, out var pestana) && pestana is bool favoritos)
            PestanaFavoritos = favoritos;
    }

    private void MostrarLista(bool favoritos)
    {
        PestanaFavoritos = favoritos;
        Sesion.EstadoVistas[ClavePestana] = favoritos;
    }
}
