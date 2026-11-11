using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Carrito : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    private bool procesando;
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CompraServicio Compras { get; set; } = default!;


    private decimal CostoEnvio => Compras.Resumen?.CostoEnvio ?? 0;
    private decimal Productos => Compras.Resumen?.SubtotalProductos ?? 0;
    private decimal Subtotal => Productos - (Compras.Resumen?.DescuentoLibros ?? 0);
    private List<Libro> Complementarios = [];

    protected override async Task CargarDatos()
    {
        Complementarios.Clear();
        if (Sesion.Usuario is null)
            return;
        Compras.MostrarCarrito(await Api.Get<ResumenCompraDTO>("carrito"));
        await CargarComplementarios();
    }

    private async Task CargarComplementarios()
    {
        var idsLibros = Compras.Resumen!.Items.Select(item => item.IdLibro).ToList();
        var datos = await Api.Post<List<TarjetaLibroDTO>>("recomendaciones/complementarios", idsLibros);
        Complementarios = datos.Select(Presentacion.Libro).ToList();
    }
    private async Task Cambiar(int id, int cantidad)
    {
        if (procesando) return;
        procesando = true;
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
        finally
        {
            procesando = false;
        }
    }

    private async Task Vaciar()
    {
        if (procesando) return;
        procesando = true;
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
        finally
        {
            procesando = false;
        }
    }

    private void Continuar()
    {
        if (Sesion.RequerirAcceso())
            Navegacion.NavigateTo("/compra");
    }

}
