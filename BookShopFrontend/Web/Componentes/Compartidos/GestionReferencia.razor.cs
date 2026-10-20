using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class GestionReferencia
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    [Parameter, EditorRequired]
    public Referencia Referencia { get; set; } = null!;

    private bool Mostrar;
    private string Nombre = "", Descripcion = "", ErrorFormulario = "";
    private string Singular => Referencia.Tipo switch
    {
        "autores" => "autor",
        "categorias" => "categoría",
        _ => "editorial"
    };

    private void Abrir()
    {
        Nombre = Referencia.Nombre;
        Descripcion = Referencia.Descripcion;
        ErrorFormulario = "";
        Mostrar = true;
    }

    private void Cerrar() => Mostrar = false;
    private void AlternarActivo()
    {
        Referencia.Activo = !Referencia.Activo;
        Estado.Notificar();
        Estado.Avisar(Referencia.Activo ? "Referencia activada" : "Referencia desactivada");
    }

    private void Guardar()
    {
        if (string.IsNullOrWhiteSpace(Nombre) || string.IsNullOrWhiteSpace(Descripcion))
        {
            ErrorFormulario = "Completa los campos obligatorios.";
            return;
        }

        var NombreAnterior = Referencia.Nombre;
        var NuevoNombre = Nombre.Trim();
        // Mantiene las etiquetas relacionadas en los datos de presentación.
        foreach (var Libro in Estado.Libros)
        {
            if (Referencia.Tipo == "autores" && Libro.Autor == NombreAnterior)
                Libro.Autor = NuevoNombre;
            else if (Referencia.Tipo == "categorias" && Libro.Categoria == NombreAnterior)
                Libro.Categoria = NuevoNombre;
            else if (Referencia.Tipo == "editoriales" && Libro.Editorial == NombreAnterior)
                Libro.Editorial = NuevoNombre;
        }

        Referencia.Nombre = NuevoNombre;
        Referencia.Descripcion = Descripcion.Trim();
        Mostrar = false;
        Estado.Notificar();
        Estado.Avisar("Cambios guardados");
    }
}

