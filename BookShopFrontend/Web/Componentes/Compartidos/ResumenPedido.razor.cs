using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class ResumenPedido
{

    [Parameter, EditorRequired]
    public Pedido Pedido { get; set; } = new();
}
