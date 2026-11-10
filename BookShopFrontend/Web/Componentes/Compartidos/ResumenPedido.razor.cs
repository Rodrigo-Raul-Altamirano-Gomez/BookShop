using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class ResumenPedido
{
    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Parameter, EditorRequired]
    public Pedido Pedido { get; set; } = new();
}
