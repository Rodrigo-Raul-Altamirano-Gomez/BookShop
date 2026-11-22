using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Admin;

// Los dos reportes tienen el mismo formulario; el período empieza en el mes actual
public partial class AdminReportes
{
    [Inject] private IJSRuntime JS { get; set; } = default!;

    private readonly List<Formulario> formularios = new()
    {
        new Formulario("libros", "Libros por monto vendido", "Libros ordenados por lo que vendieron en el período, con su categoría, autor y editorial.", "bi-book"),
        new Formulario("clientes", "Clientes por monto gastado", "Todos los clientes, incluidos los que no compraron, ordenados por su gasto en el período.", "bi-people")
    };

    // El PDF lo entrega el front en /reportes/{tipo} (DocumentosEndpoints), que lo pide al API
    private async Task Generar(Formulario formulario)
    {
        formulario.Generando = true;
        string url = $"reportes/{formulario.Tipo}?desde={formulario.Desde:yyyy-MM-dd}&hasta={formulario.Hasta:yyyy-MM-dd}";
        if (formulario.Limite != null)
        {
            url += "&limite=" + formulario.Limite;
        }
        await JS.InvokeVoidAsync("abrirPdf", url);
        formulario.Generando = false;
    }

    private class Formulario
    {
        public string Tipo { get; }
        public string Titulo { get; }
        public string Descripcion { get; }
        public string Icono { get; }
        public DateOnly? Desde { get; set; } = new DateOnly(DateTime.Today.Year, DateTime.Today.Month, 1);
        public DateOnly? Hasta { get; set; } = new DateOnly(DateTime.Today.Year, DateTime.Today.Month, 1).AddMonths(1).AddDays(-1);
        public int? Limite { get; set; }
        public bool Generando { get; set; }

        public Formulario(string tipo, string titulo, string descripcion, string icono)
        {
            Tipo = tipo;
            Titulo = titulo;
            Descripcion = descripcion;
            Icono = icono;
        }
    }
}
