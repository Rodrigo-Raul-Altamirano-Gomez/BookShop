using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Compra;

public partial class Compra : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    private PreparacionCompraDTO? preparacion;
    private SeleccionCompraDTO Seleccion => preparacion!.Seleccion;
    private int indiceDireccion, indiceMetodo, indiceCupon;
    private bool formularioDireccion, formularioMetodo, procesando;
    private string? errorPago;
    private List<CuponDTO?> Cupones
    {
        get
        {
            // La primera opción permite comprar sin cupón.
            var cupones = new List<CuponDTO?> { null };
            cupones.AddRange(Cuenta.Cupones.Where(cupon => cupon.Estado == "DISPONIBLE"));
            return cupones;
        }
    }
    private IEnumerable<CuponDTO?> CuponesVisibles => Cupones.Skip(indiceCupon).Take(2);

    protected override async Task CargarDatos()
    {
        preparacion = null;
        if (Sesion.Usuario == null)
            return;
        await Cuenta.CargarDatosCompra();
        MostrarPreparacion(await Api.Post<PreparacionCompraDTO>("pedidos/preparacion"));
    }

    private void MostrarPreparacion(PreparacionCompraDTO preparacion)
    {
        this.preparacion = preparacion;
        Compras.MostrarCarrito(preparacion.Resumen);
    }
    private async Task GuardarDireccion(DatosDireccionEnvioDTO direccion)
    {
        int id = await Cuenta.GuardarDireccion(direccion);
        formularioDireccion = false;
        indiceDireccion = Cuenta.Direcciones.FindIndex(d => d.Id == id);
        await Seleccionar(id, Seleccion.IdMetodoPago, Seleccion.IdCupon);
    }

    private async Task GuardarMetodo(DatosMetodoPagoDTO metodo)
    {
        int id = await Cuenta.GuardarMetodo(metodo);
        formularioMetodo = false;
        indiceMetodo = Cuenta.Metodos.FindIndex(m => m.Id == id);
        await Seleccionar(Seleccion.IdDireccionEnvio, id, Seleccion.IdCupon);
    }

    private async Task Seleccionar(int direccion, int metodo, int cupon)
    {
        procesando = true;
        errorPago = null;
        try
        {
            MostrarPreparacion(await Api.Put<PreparacionCompraDTO>("pedidos/preparacion",
                new SeleccionCompraDTO
                {
                    IdDireccionEnvio = direccion,
                    IdMetodoPago = metodo,
                    IdCupon = cupon
                }));
        }
        catch (InvalidOperationException e)
        {
            errorPago = e.Message;
        }
        procesando = false;
    }

    private async Task Confirmar()
    {
        if (preparacion?.PuedePagar != true)
            return;
        procesando = true;
        errorPago = null;
        DetallePedidoDTO? detalle = null;
        try
        {
            detalle = await Api.Post<DetallePedidoDTO>("pedidos/confirmacion");
        }
        catch (InvalidOperationException e)
        {
            errorPago = e.Message;
        }

        if (detalle != null)
        {
            // El pedido ya fue confirmado; no se envía otra confirmación ni se altera su detalle.
            Compras.MostrarCarrito(new ResumenCompraDTO());
            preparacion = null;
            Navegacion.NavigateTo($"/confirmacion/{detalle.Pedido.Id}");
        }
        else if (Sesion.Usuario != null)
        {
            // Si negocio actualizó los importes, mostrar la preparación vigente antes del reintento.
            try
            {
                MostrarPreparacion(await Api.Get<PreparacionCompraDTO>("pedidos/preparacion"));
            }
            catch (InvalidOperationException)
            {
                preparacion = null;
            }
        }
        procesando = false;
    }
}
