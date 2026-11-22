using BookShopWeb.Code;
using BookShopWeb.Models;
using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Pages.Tienda;

public partial class Confirmacion
{
    [Parameter] public int Id { get; set; }

    private Pedido? pedido;

    protected override async Task OnParametersSetAsync()
    {
        PedidoDetalle? detalle = await new HttpClientUtils<PedidoDetalle>().Get($"pedidos/{Id}");
        pedido = detalle!.Pedido;
    }
}
