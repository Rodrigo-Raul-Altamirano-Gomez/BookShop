using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Carrito : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    private decimal CostoEnvio => Compras.Resumen?.CostoEnvio ?? 0;
    private decimal Productos => Compras.Resumen?.SubtotalProductos ?? 0;
    private decimal Subtotal => Productos - (Compras.Resumen?.DescuentoLibros ?? 0);
    private List<Libro> Complementarios = [];

    protected override async Task OnInitializedAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {

            Sesion.Cambiado += Actualizar;
            if (Sesion.Usuario is not null)
            {
                Compras.MostrarCarrito(await Api.Get<ResumenCompraDTO>("carrito"));
                await CargarComplementarios();
            }
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }
    private async Task CargarComplementarios() => Complementarios = (await Api.Post<List<TarjetaLibroDTO>>("recomendaciones/complementarios", Compras.Carrito.Keys.ToList())).Select(Presentacion.Libro).ToList();
    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private async Task Cambiar(int id, int cantidad)
    {
        try
        {
            await Compras.CambiarCantidad(id, cantidad);
            await CargarComplementarios();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }

    private async Task Vaciar()
    {
        try
        {
            await Compras.VaciarCarrito();
            Complementarios.Clear();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private void Continuar()
    {
        if (Sesion.RequerirAcceso())
            Navegacion.NavigateTo("/compra");
    }

    public void Dispose() => Sesion.Cambiado -= Actualizar;
}

