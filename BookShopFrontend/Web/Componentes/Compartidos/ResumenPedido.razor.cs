using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class ResumenPedido
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Parameter, EditorRequired]
    public Pedido Pedido { get; set; } = new();
}

