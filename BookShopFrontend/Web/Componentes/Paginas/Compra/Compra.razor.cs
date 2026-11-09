using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Compra : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CompraServicio Compras { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Inject] private SesionUsuario Sesion { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
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
    private List<Cupon?> Cupones => new Cupon?[] { null }.Concat(Cuenta.Cupones.Where(c => c.Estado == "Disponible")).ToList();
    private IEnumerable<Cupon?> CuponesVisibles => Cupones.Skip(indiceCupon).Take(2);

    protected override async Task OnInitializedAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {

            Sesion.Cambiado += Actualizar;
            if (Sesion.Usuario is null)
                return;

            await Cuenta.CargarDatosCompra();
            MostrarPreparacion(await Api.Post<PreparacionCompraDTO>("pedidos/preparacion"));
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

    private void MostrarPreparacion(PreparacionCompraDTO preparacion)
    {
        Compras.Preparacion = preparacion;
        direccionId = preparacion.Seleccion.IdDireccionEnvio == 0 ? null : preparacion.Seleccion.IdDireccionEnvio;
        metodoId = preparacion.Seleccion.IdMetodoPago == 0 ? null : preparacion.Seleccion.IdMetodoPago;
        cuponId = preparacion.Seleccion.IdCupon == 0 ? null : preparacion.Seleccion.IdCupon;
        Compras.MostrarCarrito(preparacion.Resumen);
    }
    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void AbrirDireccion() => formularioDireccion = true;
    private void AbrirMetodo() => formularioMetodo = true;
    private void CerrarDireccion() => formularioDireccion = false;
    private void CerrarMetodo() => formularioMetodo = false;
    private async Task GuardarDireccion(Direccion direccion)
    {
        try
        {
            await Cuenta.GuardarDireccion(direccion);
            formularioDireccion = false;
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }
    private async Task GuardarMetodo(MetodoPago metodo)
    {
        try
        {
            await Cuenta.GuardarMetodo(metodo);
            formularioMetodo = false;
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
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
        catch (InvalidOperationException e) { errorPago = e.Message; }
        finally { procesando = false; }
    }
    private Task SeleccionarCupon(Cupon? cupon) => Seleccionar(direccionId, metodoId, cupon?.Id);

    private async Task Confirmar()
    {
        if (procesando || Compras.Preparacion?.PuedePagar != true)
            return;
        procesando = true;
        errorPago = null;
        try
        {
            var detalle = await Api.Post<DetallePedidoDTO>("pedidos/confirmacion");
            Compras.Pedidos.Insert(0, Presentacion.Pedido(detalle));
            // El pedido ya fue confirmado; no se envía otra confirmación ni se altera su detalle.
            Compras.MostrarCarrito(new ResumenCompraDTO
            {
                Items = [],
                SubtotalProductos = 0,
                DescuentoLibros = 0,
                IdCupon = 0,
                PorcentajeCupon = 0,
                DescuentoCupon = 0,
                PorcentajeEnvio = 0,
                CostoEnvio = 0,
                Total = 0,
                CantidadLibros = 0
            });
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
                catch (InvalidOperationException) { Compras.Preparacion = null; }
            }
        }
        finally { procesando = false; }
    }
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}
