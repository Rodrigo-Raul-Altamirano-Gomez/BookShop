using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Carrito : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    private bool procesando;
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CompraServicio Compras { get; set; } = default!;


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
        procesando = true;
        try
        {
            await Compras.CambiarCantidad(id, cantidad);
            await CargarComplementarios();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task Vaciar()
    {
        procesando = true;
        try
        {
            await Compras.VaciarCarrito();
            Complementarios.Clear();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private void Continuar()
    {
        if (Sesion.RequerirAcceso())
            Navegacion.NavigateTo("/compra");
    }

}
