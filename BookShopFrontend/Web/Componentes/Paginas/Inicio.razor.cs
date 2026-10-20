using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas;

public partial class Inicio : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;
    private IEnumerable<Referencia> Categorias => Estado.Referencias.Where(r => r.Tipo == "categorias" && r
        .Activo).Take(6);
    private IEnumerable<Referencia> Autores => Estado.Referencias.Where(r => r.Tipo == "autores" && r
        .Activo).Take(6);
    private IEnumerable<Referencia> Editoriales => Estado.Referencias.Where(r => r.Tipo == "editoriales"
        && r.Activo).Take(6);
    private IEnumerable<Libro> LibrosNuevos => Estado.Libros.Where(b => b.Activo).OrderByDescending(b => b
        .Id).Take(8);
    private IEnumerable<Libro> MasVendidos => Estado.Libros.Where(b => b.Activo).Take(8);
    private IEnumerable<Libro> LibrosPopulares => Estado.Libros.Where(b => b.Activo).Skip(8).Concat(Estado
        .Libros.Take(8)).Take(8);
    private IEnumerable<Libro> LibrosPersonales => Estado.Libros.Where(b => b.Activo).Skip(4).Take(8);

    protected override void OnInitialized() => Estado.Cambiado += Actualizar;
    private void Actualizar() => InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= Actualizar;
}

