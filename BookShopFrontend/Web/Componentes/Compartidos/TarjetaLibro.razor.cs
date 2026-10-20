using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class TarjetaLibro
{
    [Parameter, EditorRequired]
    public Libro Libro { get; set; } = new();
}

