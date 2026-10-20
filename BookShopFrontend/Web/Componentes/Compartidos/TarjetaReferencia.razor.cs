using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class TarjetaReferencia
{
    [Parameter, EditorRequired]
    public Referencia Referencia { get; set; } = new();
}

