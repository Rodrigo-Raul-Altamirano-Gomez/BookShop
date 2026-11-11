using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Compra : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    private int? direccionId, metodoId, cuponId;
    private int indiceDireccion, indiceMetodo, indiceCupon;
    private bool formularioDireccion, formularioMetodo, procesando;
    private string? errorPago;
    private ResumenCompraDTO? Resumen => Compras.Preparacion?.Resumen;
    private decimal Productos => Resumen?.SubtotalProductos ?? 0;
    private decimal Subtotal => Productos - (Resumen?.DescuentoLibros ?? 0);
    private decimal DescuentoCupon => Resumen?.DescuentoCupon ?? 0;
    private decimal CostoEnvio => Resumen?.CostoEnvio ?? 0;
    private decimal Total => Resumen?.Total ?? 0;
    private List<Cupon?> Cupones
    {
        get
        {
            // La primera opción permite comprar sin cupón.
            var cupones = new List<Cupon?> { null };
            cupones.AddRange(Cuenta.Cupones.Where(cupon => cupon.Estado == "Disponible"));
            return cupones;
        }
    }
    private IEnumerable<Cupon?> CuponesVisibles => Cupones.Skip(indiceCupon).Take(2);

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario is null)
            return;
        await Cuenta.CargarDatosCompra();
        MostrarPreparacion(await Api.Post<PreparacionCompraDTO>("pedidos/preparacion"));
    }

    private void MostrarPreparacion(PreparacionCompraDTO preparacion)
    {
        Compras.Preparacion = preparacion;
        direccionId = preparacion.Seleccion.IdDireccionEnvio == 0 ? null : preparacion.Seleccion.IdDireccionEnvio;
        metodoId = preparacion.Seleccion.IdMetodoPago == 0 ? null : preparacion.Seleccion.IdMetodoPago;
        cuponId = preparacion.Seleccion.IdCupon == 0 ? null : preparacion.Seleccion.IdCupon;
        Compras.MostrarCarrito(preparacion.Resumen);
    }
    private async Task GuardarDireccion(Direccion direccion)
    {
        int id = await Cuenta.GuardarDireccion(direccion);
        formularioDireccion = false;
        indiceDireccion = Cuenta.Direcciones.FindIndex(d => d.Id == id);
        await Seleccionar(id, metodoId, cuponId);
    }

    private async Task GuardarMetodo(MetodoPago metodo)
    {
        int id = await Cuenta.GuardarMetodo(metodo);
        formularioMetodo = false;
        indiceMetodo = Cuenta.Metodos.FindIndex(m => m.Id == id);
        await Seleccionar(direccionId, id, cuponId);
    }

    private async Task Seleccionar(int? direccion, int? metodo, int? cupon)
    {
        if (procesando)
            return;
        procesando = true;
        errorPago = null;
        try
        {
            MostrarPreparacion(await Api.Put<PreparacionCompraDTO>("pedidos/preparacion",
                new SeleccionCompraDTO
                {
                    IdDireccionEnvio = direccion ?? 0,
                    IdMetodoPago = metodo ?? 0,
                    IdCupon = cupon ?? 0
                }));
        }
        catch (InvalidOperationException e)
        {
            errorPago = e.Message;
        }
        finally
        {
            procesando = false;
        }
    }

    private async Task Confirmar()
    {
        if (procesando || Compras.Preparacion?.PuedePagar != true)
            return;
        procesando = true;
        errorPago = null;
        try
        {
            var detalle = await Api.Post<DetallePedidoDTO>("pedidos/confirmacion");
            // El pedido ya fue confirmado; no se envía otra confirmación ni se altera su detalle.
            Compras.MostrarCarrito(new ResumenCompraDTO());
            Compras.Preparacion = null;
            Navegacion.NavigateTo($"/confirmacion/{detalle.Pedido.Id}");
        }
        catch (InvalidOperationException e)
        {
            errorPago = e.Message;
            // Si negocio actualizó los importes, mostrar la preparación vigente antes del reintento.
            if (Sesion.Usuario is not null)
            {
                try
                {
                    MostrarPreparacion(await Api.Get<PreparacionCompraDTO>("pedidos/preparacion"));
                }
                catch (InvalidOperationException)
                { Compras.Preparacion = null; }
            }
        }
        finally
        {
            procesando = false;
        }
    }
}
