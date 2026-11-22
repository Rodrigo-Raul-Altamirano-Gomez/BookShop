using Microsoft.AspNetCore.Components;

namespace BookShopWeb.Components.Shared;

public partial class QuantityStepper
{
    [Parameter] public int Valor { get; set; } = 1;
    [Parameter] public EventCallback<int> ValorChanged { get; set; }
    [Parameter] public int Maximo { get; set; } = 1;
    [Parameter] public bool Pequeno { get; set; }
    [Parameter] public bool Deshabilitado { get; set; }
    // aria-label del grupo, por ejemplo "Cantidad de La casa de los relojes"
    [Parameter] public string Etiqueta { get; set; } = "Cantidad";

    // Al escribir un número fuera de rango, el campo se vuelve a dibujar con el valor válido
    private int version;

    private async Task Cambiar(int nuevo)
    {
        nuevo = Math.Clamp(nuevo, 1, Math.Max(1, Maximo));
        version++;
        if (nuevo != Valor)
        {
            await ValorChanged.InvokeAsync(nuevo);
        }
    }

    private async Task Escribir(ChangeEventArgs e)
    {
        int escrito = int.TryParse(e.Value?.ToString(), out int numero) ? numero : Valor;
        await Cambiar(escrito);
    }
}
