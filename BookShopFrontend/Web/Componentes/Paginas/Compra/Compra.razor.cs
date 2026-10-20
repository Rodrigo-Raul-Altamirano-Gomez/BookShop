using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class Compra : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    private int? direccionId, metodoId, cuponId;
    private int indiceDireccion, indiceMetodo, indiceCupon;
    private bool formularioDireccion, formularioMetodo, procesando;
    private bool rechazoMostrado;
    private string? errorPago;
    private const decimal EnvioEjemplo = 7.99m;
    private decimal Productos => Estado.Carrito.Sum(line => (Estado.BuscarLibro(line.Key)?
        .PrecioOriginal ?? 0) * line.Value);
    private decimal Subtotal => Estado.Carrito.Sum(line => (Estado.BuscarLibro(line.Key)?
        .Precio ?? 0) * line.Value);
    // Importes ilustrativos: aquí no se reproducen las fórmulas ni condiciones del backend.
    private decimal DescuentoEjemplo => cuponId is null ? 0 : 10m;
    private decimal Total => Math.Max(0, Subtotal - DescuentoEjemplo + EnvioEjemplo);
    private List<Cupon?> Cupones => new Cupon?[]
    {
        null
    }.Concat(Estado.Cupones.Where(c => c.Estado == "Disponible")).ToList();
    private IEnumerable<Cupon?> CuponesVisibles => Cupones.Skip(indiceCupon).Take(2);

    protected override void OnInitialized()
    {
        direccionId = Estado.Direcciones.FirstOrDefault(a => a.Predeterminada)?.Id;
        metodoId = Estado.Metodos.FirstOrDefault(m => m.Predeterminada)?.Id;
        Estado.Cambiado += Actualizar;
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void AbrirDireccion() => formularioDireccion = true;
    private void AbrirMetodo() => formularioMetodo = true;
    private void CerrarDireccion() => formularioDireccion = false;
    private void CerrarMetodo() => formularioMetodo = false;
    private void GuardarDireccion(Direccion address)
    {
        address.Id = Estado.Direcciones.Select(a => a.Id).DefaultIfEmpty().Max() + 1;
        if (address.Predeterminada)
            foreach (var item in Estado.Direcciones)
                item.Predeterminada = false;
        Estado.Direcciones.Add(address);
        direccionId = address.Id;
        indiceDireccion = Math.Max(0, Estado.Direcciones.Count - 2);
        formularioDireccion = false;
        Estado.Avisar("Dirección añadida.");
    }

    private void GuardarMetodo(MetodoPago method)
    {
        method.Id = Estado.Metodos.Select(m => m.Id).DefaultIfEmpty().Max() + 1;
        if (method.Predeterminada)
            foreach (var item in Estado.Metodos)
                item.Predeterminada = false;
        Estado.Metodos.Add(method);
        metodoId = method.Id;
        indiceMetodo = Math.Max(0, Estado.Metodos.Count - 2);
        formularioMetodo = false;
        Estado.Avisar("Método añadido.");
    }

    private void SeleccionarCupon(Cupon? coupon)
    {
        // El segundo cupón representa el estado visual de una selección rechazada.
        if (coupon?.Id == 2)
        {
            Estado.Avisar("Este cupón requiere una compra mínima de " + EstadoDemostracion.Dinero(coupon
                .Minimo) + ".");
            return;
        }

        cuponId = coupon?.Id;
    }

    private async Task Confirmar()
    {
        if (procesando || direccionId is null || metodoId is null)
            return;
        procesando = true;
        await Task.Delay(600);
        // Un ejemplo fijo para revisar el rechazo sin implementar una simulación de negocio.
        if (metodoId == 3 && !rechazoMostrado)
        {
            rechazoMostrado = true;
            procesando = false;
            errorPago = "El pago fue rechazado. Puedes volver a intentarlo o elegir otro método.";
            return;
        }

        var pedido = new Pedido
        {

            Id = Estado.Pedidos.Select(o => o.Id).DefaultIfEmpty().Max() + 1,

            FechaPago = DateTimeOffset.UtcNow.ToOffset(TimeSpan.FromHours(-5)).DateTime,

            EntregaEstimada = DateTimeOffset.UtcNow.ToOffset(TimeSpan.FromHours(-5)).DateTime.AddMinutes(5),

            Estado = "En almacén",

            Direccion = Estado.Direcciones.First(a => a.Id == direccionId),

            Metodo = Estado.Metodos.First(m => m.Id == metodoId),

            Productos = Productos,

            DescuentoLibro = Productos - Subtotal,

            Subtotal = Subtotal,

            DescuentoCupon = DescuentoEjemplo,

            Envio = EnvioEjemplo,

            Total = Total,

            Lineas = Estado.Carrito.Select(line =>
            {
                var book = Estado.BuscarLibro(line.Key)!;
                return new LineaPedido
                {

                    IdLibro = line.Key,

                    Cantidad = line.Value,

                    PrecioOriginal = book.PrecioOriginal,

                    Precio = book.Precio,

                    Descuento = book.Descuento

                };
            }).ToList()

        };
        Estado.Pedidos.Insert(0, pedido);
        Estado.Carrito.Clear();
        Estado.Notificar();
        Navegacion.NavigateTo($"/confirmacion/{pedido.Id}");
    }

    public void Dispose() => Estado.Cambiado -= Actualizar;
}

