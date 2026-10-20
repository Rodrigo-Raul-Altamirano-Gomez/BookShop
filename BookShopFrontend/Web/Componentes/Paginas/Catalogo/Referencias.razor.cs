using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class Referencias : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";
    private string Titulo => Tipo == "autores" ? "Autores" : Tipo == "editoriales" ? "Editoriales"
        : "Categorías";

    private string orden = "nombre", tipoAnterior = "";
    private int pagina = 1;
    private List<Referencia> Resultados => (orden == "reciente" ? Estado.Referencias.Where(r => r
        .Tipo == Tipo && r.Activo).OrderByDescending(r => r.Id) : Estado.Referencias.Where(r => r
            .Tipo == Tipo && r.Activo).OrderBy(r => r.Nombre)).ToList();
    private int TotalPaginas => Math.Max(1, (Resultados.Count + 11) / 12);
    private IEnumerable<Referencia> ReferenciasVisibles => Resultados.Skip((pagina - 1) * 12).Take(12);

    protected override void OnParametersSet()
    {
        if (tipoAnterior != Tipo)
        {
            GuardarVista();
            tipoAnterior = Tipo;
            if (Estado.EstadoVistas.TryGetValue($"referencias:{Tipo}", out var guardado)
                && guardado is VistaReferencias vista)
            {
                orden = vista.Orden;
                pagina = vista.Pagina;
            }
            else
            {
                orden = "nombre";
                pagina = 1;
            }
        }

        pagina = Math.Min(pagina, TotalPaginas);
    }

    private void GuardarVista()
    {
        if (tipoAnterior.Length == 0)
            return;
        Estado.EstadoVistas[$"referencias:{tipoAnterior}"] = new VistaReferencias
        {

            Orden = orden,

            Pagina = pagina

        };
    }

    public void Dispose() => GuardarVista();
    private class VistaReferencias
    {
        public string Orden { get; set; } = "nombre";
        public int Pagina { get; set; } = 1;
    }

    private void CambiarOrden() => pagina = 1;
    private void CambiarPagina(int value) => pagina = value;
}

