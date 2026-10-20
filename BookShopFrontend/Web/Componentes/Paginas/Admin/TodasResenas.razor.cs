using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Admin;

public partial class TodasResenas : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    private string Orden = "recent";
    private int Pagina = 1;
    private const int TamanoPagina = 10;
    private IEnumerable<Resena> Filtrados
    {
        get
        {
            var Resultado = Estado.Resenas.Where(r => r.Activo);
            return Orden switch
            {
                "votes-asc" => Resultado.OrderBy(r => r.Votos),
                "votes-desc" => Resultado.OrderByDescending(r => r.Votos),
                "stars-asc" => Resultado.OrderBy(r => r.Puntuacion),
                "stars-desc" => Resultado.OrderByDescending(r => r.Puntuacion),
                _ => Resultado.OrderByDescending(r => r.Fecha)
            };
        }
    }

    private IEnumerable<Resena> ElementosPagina => Filtrados.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Filtrados.Count() / (double)TamanoPagina));

    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    private void ActualizarVista() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private void ReiniciarPagina() => Pagina = 1;
    private void CambiarPagina(int Siguiente) => Pagina = Siguiente;
    private string MostrarEstrellas(int Cantidad) => new string('★', Cantidad) + new string('☆',
         5 - Cantidad);
    private void Eliminar(Resena Resena)
    {
        Resena.Activo = false;
        Pagina = Math.Min(Pagina, TotalPaginas);
        Estado.Notificar();
        Estado.Avisar("Reseña eliminada");
    }
}

