using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Compra;

public partial class DetallePedido : IDisposable
{
    [Microsoft.AspNetCore.Components.Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id
    {
        get; set;
    }

    private Pedido? pedido;
    private bool confirmacion;
    protected override void OnInitialized() => Sesion.Cambiado += Actualizar;
    protected override void OnParametersSet()
    {
        pedido = Compras.Pedidos.FirstOrDefault(o => o.Id == Id);
        confirmacion = Navegacion.Uri.Contains("/confirmacion/");
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private static string ClaseEstado(string estado) => "success";
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}
