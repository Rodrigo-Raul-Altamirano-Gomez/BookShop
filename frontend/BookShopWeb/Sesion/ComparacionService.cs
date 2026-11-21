using BookShopWeb.Models;
using Microsoft.JSInterop;
using Newtonsoft.Json;

namespace BookShopWeb.Sesion;

// Selección de comparación (RF09.7–10). Vive en la sesión del navegador: sobrevive a recargas y pestañas,
// y se pierde al cerrar el navegador.
public class ComparacionService
{
    private readonly IJSRuntime js;
    private readonly AvisoService aviso;
    private bool cargado;

    public List<LibroComparado> Libros { get; private set; } = new();
    public event Action? Cambio;
    // Con dos libros elegidos, la barra pregunta cuál reemplazar
    public event Action<LibroComparado>? ReemplazoPedido;

    public ComparacionService(IJSRuntime js, AvisoService aviso)
    {
        this.js = js;
        this.aviso = aviso;
    }

    public async Task Cargar()
    {
        if (cargado) return;
        string? guardado = await js.InvokeAsync<string?>("leerSesion", "bk-comparacion");
        if (!string.IsNullOrEmpty(guardado))
        {
            Libros = JsonConvert.DeserializeObject<List<LibroComparado>>(guardado) ?? new();
        }
        cargado = true;
        Cambio?.Invoke();
    }

    public async Task Agregar(Libro libro)
    {
        LibroComparado nuevo = new LibroComparado(libro.Id, libro.Titulo);
        if (Libros.Any(l => l.Id == libro.Id))
        {
            aviso.Mostrar("Este libro ya está en la comparación");
            return;
        }
        if (Libros.Count == 2)
        {
            ReemplazoPedido?.Invoke(nuevo);
            return;
        }
        Libros.Add(nuevo);
        await Guardar();
        aviso.Mostrar("Agregado a la comparación");
    }

    public async Task Reemplazar(int idAnterior, LibroComparado nuevo)
    {
        int posicion = Libros.FindIndex(l => l.Id == idAnterior);
        Libros[posicion] = nuevo;
        await Guardar();
        aviso.Mostrar("Comparación actualizada");
    }

    public async Task Quitar(int id)
    {
        Libros.RemoveAll(l => l.Id == id);
        await Guardar();
    }

    private async Task Guardar()
    {
        await js.InvokeVoidAsync("guardarSesion", "bk-comparacion", JsonConvert.SerializeObject(Libros));
        Cambio?.Invoke();
    }
}

public record LibroComparado(int Id, string Titulo);
