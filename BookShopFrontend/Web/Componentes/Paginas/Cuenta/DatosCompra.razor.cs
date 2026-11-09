using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class DatosCompra
{
    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario is not null)
            try
            {
                await Cuenta.CargarDatosCompra();
                Sesion.Notificar();
            }
            catch (InvalidOperationException e)
            {
                Sesion.Error = e.Message;
                Sesion.Avisar(e.Message);
            }
    }
    private string Pestana { get; set; } = "addresses";
    private string ModalAbierto { get; set; } = "";
    private Direccion? DireccionEnEdicion
    {
        get; set;
    }
    private MetodoPago? MetodoEnEdicion
    {
        get; set;
    }
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 10;
    private int Cantidad => Sesion.Usuario is null ? 0 : Pestana == "addresses" ? Cuenta.Direcciones.Count
        : Pestana == "methods" ? Cuenta.Metodos.Count : Cuenta.Cupones.Count;
    private string EtiquetaCantidad => Pestana == "addresses" ? "direcciones" : Pestana == "methods"
        ? "métodos" : "cupones";
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Cantidad / (double)TamanoPagina));
    private IEnumerable<Direccion> DireccionesPagina => Cuenta.Direcciones.Skip((
        Pagina - 1) * TamanoPagina).Take(TamanoPagina);
    private IEnumerable<MetodoPago> MetodosPagina => Cuenta.Metodos.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private IEnumerable<Cupon> CuponesPagina => Cuenta.Cupones.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);

    private static string Importe(decimal value) => Presentacion.Dinero(value);
    private static string TituloMetodo(MetodoPago metodo) => metodo.Alias.Length > 0 ? metodo.Alias
        : metodo.Tipo;
    private static string ClaseCupon(Cupon cupon) => cupon.Estado == "Disponible" ? "success" : cupon
        .Estado == "Vencido" ? "danger" : "neutral";
    private void MostrarDirecciones()
    {
        Pestana = "addresses";
        Pagina = 1;
    }

    private void MostrarMetodos()
    {
        Pestana = "methods";
        Pagina = 1;
    }

    private void MostrarCupones()
    {
        Pestana = "coupons";
        Pagina = 1;
    }

    private void CambiarPagina(int pagina) => Pagina = pagina;
    private void Anadir()
    {
        DireccionEnEdicion = null;
        MetodoEnEdicion = null;
        ModalAbierto = Pestana == "addresses" ? "address" : "method";
    }

    private void EditarDireccion(Direccion direccion)
    {
        DireccionEnEdicion = direccion;
        ModalAbierto = "address";
    }

    private void EditarMetodo(MetodoPago metodo)
    {
        MetodoEnEdicion = metodo;
        ModalAbierto = "method";
    }

    private void Cerrar()
    {
        ModalAbierto = "";
        DireccionEnEdicion = null;
        MetodoEnEdicion = null;
    }

    private async Task GuardarDireccion(Direccion direccion)
    {
        try
        {
            await Cuenta.GuardarDireccion(direccion, DireccionEnEdicion?.Id ?? 0);
            Cerrar();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }

    private async Task GuardarMetodo(MetodoPago metodo)
    {
        try
        {
            await Cuenta.GuardarMetodo(metodo, MetodoEnEdicion?.Id ?? 0);
            Cerrar();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private async Task EliminarDireccion(Direccion direccion)
    {
        try
        {
            await Api.Delete($"cuenta/direcciones/{direccion.Id}");
            await Cuenta.CargarDatosCompra();
            Pagina = Math.Min(Pagina, TotalPaginas);
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private async Task EliminarMetodo(MetodoPago metodo)
    {
        try
        {
            await Api.Delete($"cuenta/metodos/{metodo.Id}");
            await Cuenta.CargarDatosCompra();
            Pagina = Math.Min(Pagina, TotalPaginas);
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private async Task PredeterminarDireccion(Direccion direccion)
    {
        try
        {
            await Api.Put($"cuenta/direcciones/{direccion.Id}/predeterminada");
            await Cuenta.CargarDatosCompra();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private async Task PredeterminarMetodo(MetodoPago metodo)
    {
        try
        {
            await Api.Put($"cuenta/metodos/{metodo.Id}/predeterminado");
            await Cuenta.CargarDatosCompra();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }
}

