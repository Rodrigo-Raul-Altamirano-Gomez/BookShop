namespace BookShopWeb.Sesion;

// Avisos breves (UX-UI §6.10): abajo a la derecha, 4 segundos, con un enlace opcional
public class AvisoService
{
    public List<Aviso> Avisos { get; } = new();
    public event Action? Cambio;

    public async void Mostrar(string texto, string? enlaceTexto = null, string? enlaceUrl = null)
    {
        Aviso aviso = new Aviso(texto, enlaceTexto, enlaceUrl);
        Avisos.Add(aviso);
        Cambio?.Invoke();
        await Task.Delay(4000);
        Quitar(aviso);
    }

    public void Quitar(Aviso aviso)
    {
        if (Avisos.Remove(aviso))
        {
            Cambio?.Invoke();
        }
    }
}

public record Aviso(string Texto, string? EnlaceTexto, string? EnlaceUrl);
