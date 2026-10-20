using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Carrito : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    private const decimal EnvioEjemplo = 7.99m;
    private decimal Productos => Estado.Carrito.Sum(line => (Estado.BuscarLibro(line.Key)?
        .PrecioOriginal ?? 0) * line.Value);
    private decimal Subtotal => Estado.Carrito.Sum(line => (Estado.BuscarLibro(line.Key)?
        .Precio ?? 0) * line.Value);
    private IEnumerable<Libro> Complementarios => Estado.Libros.Where(b => !Estado.Carrito.ContainsKey(b
        .Id)).Take(4);

    protected override void OnInitialized() => Estado.Cambiado += Actualizar;
    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void Cambiar(int id, int cantidad)
    {
        if (cantidad <= 0)
            Estado.Carrito.Remove(id);
        else
            Estado.Carrito[id] = cantidad;
        Estado.Notificar();
    }

    private void Vaciar()
    {
        Estado.Carrito.Clear();
        Estado.Notificar();
    }

    private void Continuar()
    {
        if (Estado.RequerirAcceso())
            Navegacion.NavigateTo("/compra");
    }

    public void Dispose() => Estado.Cambiado -= Actualizar;
}

