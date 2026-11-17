using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class DatosCompra
{
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario == null)
            return;
        await Cuenta.CargarDatosCompra();
    }
    private string Pestana { get; set; } = "direcciones";
    private string ModalAbierto { get; set; } = "";
    private DireccionEnvioDTO? DireccionEnEdicion { get; set; }
    private MetodoPagoDTO? MetodoEnEdicion { get; set; }
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 10;
    private int Cantidad
    {
        get
        {
            if (Sesion.Usuario == null)
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
    private IEnumerable<DireccionEnvioDTO> DireccionesPagina => Cuenta.Direcciones.Skip((
        Pagina - 1) * TamanoPagina).Take(TamanoPagina);
    private IEnumerable<MetodoPagoDTO> MetodosPagina => Cuenta.Metodos.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private IEnumerable<CuponDTO> CuponesPagina => Cuenta.Cupones.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);

    private static string ClaseCupon(CuponDTO cupon)
    {
        if (cupon.Estado == "DISPONIBLE")
            return "success";
        if (cupon.Estado == "VENCIDO")
            return "danger";
        return "neutral";
    }

    private void MostrarPestana(string pestana)
    {
        Pestana = pestana;
        Pagina = 1;
    }

    private void CambiarPagina(int pagina) => Pagina = pagina;
    private void Anadir()
    {
        DireccionEnEdicion = null;
        MetodoEnEdicion = null;
        ModalAbierto = Pestana == "direcciones" ? "direccion" : "metodo";
    }

    private void EditarDireccion(DireccionEnvioDTO direccion)
    {
        DireccionEnEdicion = direccion;
        ModalAbierto = "direccion";
    }

    private void EditarMetodo(MetodoPagoDTO metodo)
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

    private async Task GuardarDireccion(DatosDireccionEnvioDTO direccion)
    {
        await Cuenta.GuardarDireccion(direccion, DireccionEnEdicion?.Id ?? 0);
        Cerrar();
    }

    private async Task GuardarMetodo(DatosMetodoPagoDTO metodo)
    {
        await Cuenta.GuardarMetodo(metodo, MetodoEnEdicion?.Id ?? 0);
        Cerrar();
    }

    private async Task EliminarDireccion(DireccionEnvioDTO direccion)
    {
        try
        {
            await Api.Delete($"cuenta/direcciones/{direccion.Id}");
            await Cuenta.CargarDirecciones();
            Pagina = Math.Min(Pagina, TotalPaginas);
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }

    private async Task EliminarMetodo(MetodoPagoDTO metodo)
    {
        try
        {
            await Api.Delete($"cuenta/metodos/{metodo.Id}");
            await Cuenta.CargarMetodos();
            Pagina = Math.Min(Pagina, TotalPaginas);
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }

    private async Task PredeterminarDireccion(DireccionEnvioDTO direccion)
    {
        try
        {
            await Api.Put($"cuenta/direcciones/{direccion.Id}/predeterminada");
            await Cuenta.CargarDirecciones();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }

    private async Task PredeterminarMetodo(MetodoPagoDTO metodo)
    {
        try
        {
            await Api.Put($"cuenta/metodos/{metodo.Id}/predeterminado");
            await Cuenta.CargarMetodos();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }
}
