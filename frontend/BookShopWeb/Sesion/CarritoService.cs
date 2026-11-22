using BookShopWeb.Models;
using Microsoft.JSInterop;
using Newtonsoft.Json;

namespace BookShopWeb.Sesion;

// Carrito (RF11). Vive en la sesión del navegador, igual que la comparación: sobrevive a recargas y pestañas,
// y se pierde al cerrar el navegador o la sesión (AuthEndpoints borra su cookie).
// Los importes y los límites de stock los calcula el backend (pedidos/resumen).
public class CarritoService
{
    private readonly IJSRuntime js;
    private bool cargado;

    public List<ItemCarrito> Items { get; private set; } = new();
    public event Action? Cambio;

    // Cantidad de libros: la suma de las unidades (contador del encabezado y título del carrito)
    public int Cantidad => Items.Sum(i => i.Cantidad);

    public CarritoService(IJSRuntime js)
    {
        this.js = js;
    }

    public async Task Cargar()
    {
        if (cargado) return;
        string? guardado = await js.InvokeAsync<string?>("leerSesion", "bk-carrito");
        if (!string.IsNullOrEmpty(guardado))
        {
            Items = JsonConvert.DeserializeObject<List<ItemCarrito>>(guardado) ?? new();
        }
        cargado = true;
        Cambio?.Invoke();
    }

    // RF11.2: si el libro ya está en el carrito, se suma la cantidad
    public async Task Agregar(int idLibro, int cantidad)
    {
        int posicion = Items.FindIndex(i => i.IdLibro == idLibro);
        if (posicion >= 0)
        {
            Items[posicion] = new ItemCarrito(idLibro, Items[posicion].Cantidad + cantidad);
        }
        else
        {
            Items.Add(new ItemCarrito(idLibro, cantidad));
        }
        await Guardar();
    }

    public async Task CambiarCantidad(int idLibro, int cantidad)
    {
        int posicion = Items.FindIndex(i => i.IdLibro == idLibro);
        Items[posicion] = new ItemCarrito(idLibro, cantidad);
        await Guardar();
    }

    public async Task Quitar(int idLibro)
    {
        Items.RemoveAll(i => i.IdLibro == idLibro);
        await Guardar();
    }

    // Deja el carrito como lo devolvió el backend (sin los libros inactivos o agotados y con cada cantidad hasta su stock)
    public async Task Reemplazar(List<LineaCompra> lineas)
    {
        Items = lineas.Select(l => new ItemCarrito(l.Libro.Id, l.Cantidad)).ToList();
        await Guardar();
    }

    public async Task Vaciar()
    {
        Items.Clear();
        await Guardar();
    }

    private async Task Guardar()
    {
        await js.InvokeVoidAsync("guardarSesion", "bk-carrito", JsonConvert.SerializeObject(Items));
        Cambio?.Invoke();
    }
}
