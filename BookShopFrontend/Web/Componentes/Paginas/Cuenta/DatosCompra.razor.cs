using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class DatosCompra
{
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario is null)
            return;
        await Cuenta.CargarDatosCompra();
        Sesion.Notificar();
    }
    private string Pestana { get; set; } = "direcciones";
    private string ModalAbierto { get; set; } = "";
    private Direccion? DireccionEnEdicion { get; set; }
    private MetodoPago? MetodoEnEdicion { get; set; }
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 10;
    private int Cantidad
    {
        get
        {
            if (Sesion.Usuario is null)
                return 0;
            if (Pestana == "direcciones")
                return Cuenta.Direcciones.Count;
            if (Pestana == "metodos")
                return Cuenta.Metodos.Count;
            return Cuenta.Cupones.Count;
        }
    }
    private string EtiquetaCantidad
    {
        get
        {
            if (Pestana == "direcciones")
                return "direcciones";
            if (Pestana == "metodos")
                return "métodos";
            return "cupones";
        }
    }
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Cantidad / (double)TamanoPagina));
    private IEnumerable<Direccion> DireccionesPagina => Cuenta.Direcciones.Skip((
        Pagina - 1) * TamanoPagina).Take(TamanoPagina);
    private IEnumerable<MetodoPago> MetodosPagina => Cuenta.Metodos.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private IEnumerable<Cupon> CuponesPagina => Cuenta.Cupones.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);

    private static string TituloMetodo(MetodoPago metodo) => metodo.Alias.Length > 0 ? metodo.Alias
        : metodo.Tipo;
    private static string ClaseCupon(Cupon cupon)
    {
        if (cupon.Estado == "Disponible")
            return "success";
        if (cupon.Estado == "Vencido")
            return "danger";
        return "neutral";
    }

    private void MostrarDirecciones()
    {
        Pestana = "direcciones";
        Pagina = 1;
    }

    private void MostrarMetodos()
    {
        Pestana = "metodos";
        Pagina = 1;
    }

    private void MostrarCupones()
    {
        Pestana = "cupones";
        Pagina = 1;
    }

    private void CambiarPagina(int pagina) => Pagina = pagina;
    private void Anadir()
    {
        DireccionEnEdicion = null;
        MetodoEnEdicion = null;
        ModalAbierto = Pestana == "direcciones" ? "direccion" : "metodo";
    }

    private void EditarDireccion(Direccion direccion)
    {
        DireccionEnEdicion = direccion;
        ModalAbierto = "direccion";
    }

    private void EditarMetodo(MetodoPago metodo)
    {
        MetodoEnEdicion = metodo;
        ModalAbierto = "metodo";
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
            await Cuenta.CargarDirecciones();
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
            await Cuenta.CargarMetodos();
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
            await Cuenta.CargarDirecciones();
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
            await Cuenta.CargarMetodos();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }
}
