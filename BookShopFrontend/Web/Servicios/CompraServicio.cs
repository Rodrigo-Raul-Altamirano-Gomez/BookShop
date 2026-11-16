namespace Web.Servicios;

public class CompraServicio
{
    private readonly HttpClientUtils api;
    private readonly SesionUsuario Sesion;
    public ResumenCompraDTO? Resumen
    {
        get; private set;
    }
    public int CantidadCarrito => Resumen?.CantidadLibros ?? 0;

    public CompraServicio(HttpClientUtils api, SesionUsuario sesion)
    {
        this.api = api;
        Sesion = sesion;
        Sesion.Cerrada += Limpiar;
    }

    private void Limpiar()
    {
        Resumen = null;
    }

    public void MostrarCarrito(ResumenCompraDTO resumen)
    {
        Resumen = resumen;
        Sesion.Notificar();
    }

    public async Task AgregarLibro(int idLibro, int cantidad)
    {
        MostrarCarrito(await api.Post<ResumenCompraDTO>($"carrito/libros/{idLibro}?cantidad={cantidad}"));
    }

    public async Task CambiarCantidad(int idLibro, int cantidad)
    {
        MostrarCarrito(await api.Put<ResumenCompraDTO>($"carrito/libros/{idLibro}?cantidad={cantidad}"));
    }

    public async Task VaciarCarrito()
    {
        MostrarCarrito(await api.Delete<ResumenCompraDTO>("carrito"));
    }
}
