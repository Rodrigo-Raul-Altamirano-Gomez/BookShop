using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace BookShopWeb.Components.Pages.Tienda;

// RF15 y RF16 (UX-UI §8.11): dirección, método de pago y cupón; luego Pagar
public partial class Compra
{
    [Inject] private CarritoService Carrito { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private int idUsuario;
    private List<Direccion> direcciones = new();
    private List<MetodoPago> metodos = new();
    private List<Cupon> cupones = new();
    private int? idDireccion;
    private int? idMetodoPago;
    private int? idCupon;
    // El resumen que ve el cliente; al pagar se envía para comparar con los datos actuales (RF15.4)
    private ResumenCompra? resumen;
    private List<string>? cambios;
    private bool rechazado;
    private bool pagando;
    private DireccionModal modalDireccion = default!;
    private MetodoPagoModal modalMetodo = default!;

    protected override async Task OnInitializedAsync()
    {
        idUsuario = SesionUsuario.Id((await AuthState).User);
        await Carrito.Cargar();
        if (Carrito.Items.Count == 0)
        {
            Navegacion.NavigateTo("carrito");
            return;
        }
        Task<List<Direccion>?> pedidoDirecciones = new HttpClientUtils<List<Direccion>>().Get($"direcciones/usuario/{idUsuario}");
        Task<List<MetodoPago>?> pedidoMetodos = new HttpClientUtils<List<MetodoPago>>().Get($"metodos-pago/usuario/{idUsuario}/vigentes");
        Task<List<Cupon>?> pedidoCupones = new HttpClientUtils<List<Cupon>>().Get($"cupones/usuario/{idUsuario}/utilizables");
        direcciones = (await pedidoDirecciones)!;
        metodos = (await pedidoMetodos)!;
        cupones = (await pedidoCupones)!;

        // RF15.1: vienen elegidos la dirección y el método predeterminados (el backend los lista primero)
        // y el cupón que vence antes (UX-UI §8.11)
        idDireccion = direcciones.FirstOrDefault()?.Id;
        idMetodoPago = metodos.FirstOrDefault()?.Id;
        idCupon = cupones.FirstOrDefault()?.Id;
        await Recalcular();
    }

    private async Task Recalcular()
    {
        resumen = await new HttpClientUtils<ResumenCompra>().Post("pedidos/resumen",
            new SolicitudCompra(idUsuario, Carrito.Items, null, null, idCupon, null));
        await Carrito.Reemplazar(resumen!.Lineas);
    }

    private async Task ElegirCupon(int? id)
    {
        idCupon = id;
        await Recalcular();
    }

    private async Task DireccionGuardada(Direccion direccion)
    {
        direcciones = (await new HttpClientUtils<List<Direccion>>().Get($"direcciones/usuario/{idUsuario}"))!;
        idDireccion = direccion.Id;
    }

    // Un método vencido no se ofrece al pagar, así que solo se elige si está entre los vigentes
    private async Task MetodoGuardado(MetodoPago metodo)
    {
        metodos = (await new HttpClientUtils<List<MetodoPago>>().Get($"metodos-pago/usuario/{idUsuario}/vigentes"))!;
        if (metodos.Any(m => m.Id == metodo.Id))
        {
            idMetodoPago = metodo.Id;
        }
    }

    private async Task Pagar()
    {
        pagando = true;
        cambios = null;
        rechazado = false;
        try
        {
            Pedido? pedido = await new HttpClientUtils<Pedido>().Post("pedidos",
                new SolicitudCompra(idUsuario, Carrito.Items, idDireccion, idMetodoPago, idCupon, resumen));
            // RF16.2: se vacía el carrito y se muestra la confirmación
            await Carrito.Vaciar();
            Navegacion.NavigateTo($"compra/confirmacion/{pedido!.Id}");
        }
        catch (ApiException ex) when (ex.Estado == 412)
        {
            // RF15.4: se muestra el resumen actualizado con la lista de cambios, y no se continúa
            cambios = ex.Message.Split('\n').ToList();
            cupones = (await new HttpClientUtils<List<Cupon>>().Get($"cupones/usuario/{idUsuario}/utilizables"))!;
            if (!cupones.Any(c => c.Id == idCupon))
            {
                idCupon = null;
            }
            await Recalcular();
            pagando = false;
            if (resumen!.Lineas.Count == 0)
            {
                Navegacion.NavigateTo("carrito");
            }
        }
        catch (ApiException ex) when (ex.Estado == 402)
        {
            // RF15.6: no se registró nada; el carrito y las selecciones se conservan para reintentar
            rechazado = true;
            pagando = false;
        }
    }
}
