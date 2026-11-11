using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : Web.Componentes.Paginas.Cuenta.PaginaCuenta
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    [Parameter] public int Id { get; set; }
    private Pedido? pedido;
    private bool confirmacion = true;

    protected override async Task CargarDatos()
    {
        pedido = null;
        if (Sesion.Usuario is not null)
            pedido = Presentacion.Pedido(await Api.Get<DetallePedidoDTO>($"pedidos/{Id}"));
    }
}
