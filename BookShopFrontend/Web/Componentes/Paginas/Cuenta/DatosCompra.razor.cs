using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class DatosCompra
{
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
    private int Cantidad => Estado.Usuario is null ? 0 : Pestana == "addresses" ? Estado.Direcciones.Count
        : Pestana == "methods" ? Estado.Metodos.Count : Estado.Cupones.Count;
    private string EtiquetaCantidad => Pestana == "addresses" ? "direcciones" : Pestana == "methods"
        ? "métodos" : "cupones";
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Cantidad / (double)TamanoPagina));
    private IEnumerable<Direccion> DireccionesPagina => Estado.Direcciones.Skip((
        Pagina - 1) * TamanoPagina).Take(TamanoPagina);
    private IEnumerable<MetodoPago> MetodosPagina => Estado.Metodos.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private IEnumerable<Cupon> CuponesPagina => Estado.Cupones.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);

    private static string Importe(decimal value) => EstadoDemostracion.Dinero(value);
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

    private void GuardarDireccion(Direccion direccion)
    {
        if (direccion.Predeterminada)
            foreach (var elemento in Estado.Direcciones)
                elemento.Predeterminada = false;
        if (DireccionEnEdicion is null)
        {
            direccion.Id = Estado.Direcciones.Select(elemento => elemento.Id).DefaultIfEmpty().Max() + 1;
            Estado.Direcciones.Add(direccion);
        }
        else
            Estado.Direcciones[Estado.Direcciones.IndexOf(DireccionEnEdicion)] = direccion;
        Cerrar();
        Estado.Notificar();
        Estado.Avisar("Dirección guardada.");
    }

    private void GuardarMetodo(MetodoPago metodo)
    {
        if (metodo.Predeterminada)
            foreach (var elemento in Estado.Metodos)
                elemento.Predeterminada = false;
        if (MetodoEnEdicion is null)
        {
            metodo.Id = Estado.Metodos.Select(elemento => elemento.Id).DefaultIfEmpty().Max() + 1;
            Estado.Metodos.Add(metodo);
        }
        else
            Estado.Metodos[Estado.Metodos.IndexOf(MetodoEnEdicion)] = metodo;
        Cerrar();
        Estado.Notificar();
        Estado.Avisar("Método de pago guardado.");
    }

    private void EliminarDireccion(Direccion direccion)
    {
        Estado.Direcciones.Remove(direccion);
        Pagina = Math.Min(Pagina, TotalPaginas);
        Estado.Notificar();
        Estado.Avisar("Dirección eliminada.");
    }

    private void EliminarMetodo(MetodoPago metodo)
    {
        Estado.Metodos.Remove(metodo);
        Pagina = Math.Min(Pagina, TotalPaginas);
        Estado.Notificar();
        Estado.Avisar("Método de pago eliminado.");
    }

    private void PredeterminarDireccion(Direccion direccion)
    {
        foreach (var elemento in Estado.Direcciones)
            elemento.Predeterminada = elemento == direccion;
        Estado.Notificar();
        Estado.Avisar("Dirección predeterminada actualizada.");
    }

    private void PredeterminarMetodo(MetodoPago metodo)
    {
        foreach (var elemento in Estado.Metodos)
            elemento.Predeterminada = elemento == metodo;
        Estado.Notificar();
        Estado.Avisar("Método predeterminado actualizado.");
    }
}

