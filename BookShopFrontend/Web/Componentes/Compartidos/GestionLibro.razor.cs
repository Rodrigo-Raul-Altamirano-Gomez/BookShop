using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class GestionLibro
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    [Parameter, EditorRequired]
    public Libro Libro { get; set; } = null!;

    private readonly string[] NombresPestanas = ["Datos del libro", "Precio", "Stock", "Descuentos"];
    private bool Mostrar;
    private string Pestana = "Datos del libro";
    private Libro Borrador = new();
    private string Peso = "", Alto = "", Ancho = "", ErrorFormulario = "";
    private List<DescuentoLibro> Descuentos = [];
    private readonly HashSet<DescuentoLibro> DescuentosEliminados = [];
    private DescuentoLibro? DescuentoSeleccionado;
    private int PosicionDescuentos, Porcentaje;
    private DateTime InicioDescuento, FinDescuento;
    private bool EliminarDescuento;
    private void Abrir()
    {
        Borrador = new Libro
        {

            Titulo = Libro.Titulo,

            Idioma = Libro.Idioma,

            Sinopsis = Libro.Sinopsis,

            Proveedor = Libro.Proveedor,

            ISBN = Libro.ISBN,

            Anio = Libro.Anio,

            Paginas = Libro.Paginas,

            Saga = Libro.Saga,

            Tomo = Libro.Tomo,

            PrecioOriginal = Libro.PrecioOriginal,

            Stock = Libro.Stock

        };
        Peso = Libro.Peso.Replace(" kg", "");
        var Dimensiones = Libro.Dimensiones.Replace(" cm", "").Split('×');
        Alto = Dimensiones.ElementAtOrDefault(0)?.Trim() ?? "";
        Ancho = Dimensiones.ElementAtOrDefault(1)?.Trim() ?? "";
        Descuentos = Libro.Descuentos.Select(d => new DescuentoLibro
        {
            Porcentaje = d.Porcentaje,
            Inicio = d.Inicio,
            Fin = d.Fin
        }).ToList();
        DescuentosEliminados.Clear();
        Pestana = "Datos del libro";
        ErrorFormulario = "";
        PosicionDescuentos = 0;
        DescuentoSeleccionado = null;
        if (Descuentos.Count > 0)
            ElegirDescuento(Descuentos[0]);
        Mostrar = true;
    }

    private void Cerrar() => Mostrar = false;
    private void ElegirPestana(string Nombre)
    {
        ConservarEdicionDescuento();
        Pestana = Nombre;
        ErrorFormulario = "";
    }

    private void AlternarActivo()
    {
        Libro.Activo = !Libro.Activo;
        Estado.Notificar();
        Estado.Avisar(Libro.Activo ? "Libro activado" : "Libro desactivado");
    }

    private void DescuentoAnterior() => PosicionDescuentos = Math.Max(0, PosicionDescuentos - 2);
    private void DescuentoSiguiente() => PosicionDescuentos += 2;
    private void ConservarEdicionDescuento()
    {
        if (DescuentoSeleccionado is null)
            return;
        DescuentoSeleccionado.Porcentaje = Porcentaje;
        DescuentoSeleccionado.Inicio = InicioDescuento;
        DescuentoSeleccionado.Fin = FinDescuento;
        if (EliminarDescuento)
            DescuentosEliminados.Add(DescuentoSeleccionado);
        else
            DescuentosEliminados.Remove(DescuentoSeleccionado);
    }

    private void ElegirDescuento(DescuentoLibro Descuento)
    {
        ConservarEdicionDescuento();
        DescuentoSeleccionado = Descuento;
        Porcentaje = Descuento.Porcentaje;
        InicioDescuento = Descuento.Inicio;
        FinDescuento = Descuento.Fin;
        EliminarDescuento = DescuentosEliminados.Contains(Descuento);
    }

    private void AgregarDescuento()
    {
        var Descuento = new DescuentoLibro
        {

            Porcentaje = 10,

            Inicio = new(2026, 11, 1),

            Fin = new(2026, 11, 30, 23, 59, 0)

        };
        Descuentos.Add(Descuento);
        ElegirDescuento(Descuento);
        PosicionDescuentos = (Descuentos.Count - 1) / 2 * 2;
    }

    private void Guardar()
    {
        ErrorFormulario = "";
        if (string.IsNullOrWhiteSpace(Borrador.Titulo) || string.IsNullOrWhiteSpace(Borrador.Idioma)
            || string.IsNullOrWhiteSpace(Borrador.Sinopsis) || string.IsNullOrWhiteSpace(Borrador.ISBN)
                || string.IsNullOrWhiteSpace(Borrador.Proveedor))
        {
            ErrorFormulario = "Completa los campos obligatorios de Datos del libro.";
            Pestana = "Datos del libro";
            return;
        }

        if (Borrador.PrecioOriginal <= 0 || Borrador.Stock < 0 || Borrador.Paginas < 1 || Borrador.Anio < 1)
        {
            ErrorFormulario = "Introduce un precio, stock, año y número de páginas válidos.";
            return;
        }

        ConservarEdicionDescuento();
        if (Descuentos.Any(d => !DescuentosEliminados.Contains(d) && (d.Porcentaje < 1 || d
            .Porcentaje > 100 || d.Inicio == default || d.Fin <= d.Inicio)))
        {
            ErrorFormulario = ("Completa el porcentaje y las fechas de los descuentos. La fecha de "
                + "fin debe ser posterior al inicio.");
            Pestana = "Descuentos";
            return;
        }

        Descuentos.RemoveAll(d => DescuentosEliminados.Contains(d));
        Libro.Titulo = Borrador.Titulo.Trim();
        Libro.Idioma = Borrador.Idioma.Trim();
        Libro.Sinopsis = Borrador.Sinopsis.Trim();
        Libro.Proveedor = Borrador.Proveedor.Trim();
        Libro.ISBN = Borrador.ISBN.Trim();
        Libro.Anio = Borrador.Anio;
        Libro.Paginas = Borrador.Paginas;
        Libro.Saga = Borrador.Saga;
        Libro.Tomo = Borrador.Tomo;
        Libro.Peso = string.IsNullOrWhiteSpace(Peso) ? "" : Peso + " kg";
        Libro.Dimensiones = string.IsNullOrWhiteSpace(Alto) && string.IsNullOrWhiteSpace(Ancho) ? ""
            : Alto + " × " + Ancho + " cm";
        // Solo actualiza el precio ilustrativo cuando se modifica su apartado.
        if (Libro.PrecioOriginal != Borrador.PrecioOriginal)
        {
            Libro.PrecioOriginal = Borrador.PrecioOriginal;
            Libro.Precio = Math.Round(Libro.PrecioOriginal * (100 - Libro.Descuento) / 100m, 2);
        }

        Libro.Stock = Borrador.Stock;
        Libro.Descuentos = Descuentos;
        Mostrar = false;
        Estado.Notificar();
        Estado.Avisar("Cambios guardados");
    }
}

