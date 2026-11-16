using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    [Parameter] public int Id { get; set; }
    private DetallePedidoDTO? pedido;
    private string EstadoPedido => pedido?.Pedido.Estado switch
    {
        "EN_ALMACEN" => "En almacén",
        "EN_CAMINO" => "En camino",
        "ENTREGADO" => "Entregado",
        "CANCELADO" => "Cancelado",
        "DEVUELTO" => "Devuelto",
        _ => ""
    };

    protected override async Task CargarDatos()
    {
        pedido = null;
        if (Sesion.Usuario is not null)
            pedido = await Api.Get<DetallePedidoDTO>($"pedidos/{Id}");
    }
}
