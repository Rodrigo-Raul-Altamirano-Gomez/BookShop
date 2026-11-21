using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.1–6 y UX-UI §10.3: consulta, edición y alta de un libro, y sus descuentos
public partial class AdminLibro
{
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private IWebHostEnvironment Entorno { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    [Parameter] public int? Id { get; set; }
    // ?editar=1 abre directamente la edición
    [SupplyParameterFromQuery(Name = "editar")] public int? AbrirEnEdicion { get; set; }

    private Libro? libro;
    private Libro edicion = new();
    private bool editando;
    private bool guardando;
    private bool noEncontrado;
    private List<Descuento>? descuentos;
    private int descuentosVisibles = 5;
    private Descuento? descuentoAEliminar;
    private DescuentoModal modalDescuento = default!;
    private ConfirmModal confirmacion = default!;

    // Alta: referencias entre los activos (RF04.3)
    private List<Entidad> autores = new();
    private List<Entidad> categorias = new();
    private List<Entidad> editoriales = new();
    private int? idAutor;
    private int? idCategoria;
    private int? idEditorial;

    protected override async Task OnParametersSetAsync()
    {
        if (Id == null)
        {
            // Alta: solo el formulario
            libro = null;
            edicion = new Libro { Idioma = "Español", Activo = true };
            Task<List<Entidad>?> pedidoAutores = new HttpClientUtils<List<Entidad>>().Get("entidades/activas?tipo=AUTOR");
            Task<List<Entidad>?> pedidoCategorias = new HttpClientUtils<List<Entidad>>().Get("entidades/activas?tipo=CATEGORIA");
            Task<List<Entidad>?> pedidoEditoriales = new HttpClientUtils<List<Entidad>>().Get("entidades/activas?tipo=EDITORIAL");
            autores = (await pedidoAutores)!;
            categorias = (await pedidoCategorias)!;
            editoriales = (await pedidoEditoriales)!;
            return;
        }
        try
        {
            await RecargarLibro();
            await RecargarDescuentos();
            if (AbrirEnEdicion == 1)
            {
                await Editar();
            }
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
        }
    }

    private async Task RecargarLibro()
    {
        libro = await new HttpClientUtils<Libro>().Get($"libros/{Id}");
    }

    private async Task RecargarDescuentos()
    {
        descuentos = await new HttpClientUtils<List<Descuento>>().Get($"descuentos/libro/{Id}");
        // El precio final de hoy puede cambiar con un descuento nuevo o eliminado
        await RecargarLibro();
    }

    private async Task Editar()
    {
        edicion = new Libro
        {
            Id = libro!.Id, Isbn = libro.Isbn, Titulo = libro.Titulo, Sinopsis = libro.Sinopsis, Idioma = libro.Idioma,
            Paginas = libro.Paginas, AnioPublicacion = libro.AnioPublicacion, Precio = libro.Precio, Stock = libro.Stock,
            Portada = libro.Portada, Activo = libro.Activo, Autor = libro.Autor, Categoria = libro.Categoria, Editorial = libro.Editorial
        };
        editando = true;
        await JS.InvokeVoidAsync("enfocar", "#tit");
    }

    private async Task ElegirPortada(InputFileChangeEventArgs e)
    {
        edicion.Portada = await Imagenes.Guardar(e.File, Entorno.WebRootPath);
    }

    private async Task Guardar()
    {
        guardando = true;
        if (Id == null)
        {
            edicion.Autor = new Entidad { Id = idAutor!.Value };
            edicion.Categoria = new Entidad { Id = idCategoria!.Value };
            edicion.Editorial = new Entidad { Id = idEditorial!.Value };
            Libro? registrado = await new HttpClientUtils<Libro>().Post("libros", edicion);
            Aviso.Mostrar("Libro registrado");
            Navegacion.NavigateTo("admin/libros/" + registrado!.Id);
        }
        else
        {
            await new HttpClientUtils<object>().Put($"libros/{Id}", edicion);
            await RecargarLibro();
            editando = false;
            Aviso.Mostrar("Cambios guardados");
        }
        guardando = false;
    }

    private async Task PedirCambioEstado()
    {
        descuentoAEliminar = null;
        if (libro!.Activo)
        {
            await confirmacion.Mostrar($"¿Desactivar «{libro.Titulo}»?", "Dejará de mostrarse en la tienda. Lo ya registrado (pedidos, listas, reseñas) no cambia.", "Sí, desactivar", true);
        }
        else
        {
            await confirmacion.Mostrar($"¿Activar «{libro.Titulo}»?", "Volverá a mostrarse en la tienda.", "Sí, activar", false);
        }
    }

    private async Task PedirEliminarDescuento(Descuento descuento)
    {
        descuentoAEliminar = descuento;
        string mensaje = descuento.Estado == EstadoDescuento.VIGENTE
            ? $"El libro volverá a su precio de {Formato.Soles(libro!.Precio)}."
            : "El descuento programado no se aplicará.";
        await confirmacion.Mostrar($"¿Eliminar el descuento del {Formato.Fecha(descuento.FechaInicio)} al {Formato.Fecha(descuento.FechaFin)}?", mensaje, "Sí, eliminar", true);
    }

    // El mismo diálogo de confirmación sirve para el estado del libro y para eliminar un descuento
    private async Task Confirmado()
    {
        if (descuentoAEliminar != null)
        {
            await new HttpClientUtils<object>().Delete($"descuentos/{descuentoAEliminar.Id}");
            Aviso.Mostrar("Descuento eliminado");
            await RecargarDescuentos();
            return;
        }
        string accion = libro!.Activo ? "desactivar" : "activar";
        await new HttpClientUtils<object>().Put($"libros/{libro.Id}/{accion}", new { });
        Aviso.Mostrar(libro.Activo ? "Libro desactivado" : "Libro activado");
        await RecargarLibro();
    }

    private static string TextoEstado(EstadoDescuento estado) => estado switch
    {
        EstadoDescuento.VIGENTE => "Vigente",
        EstadoDescuento.PROGRAMADO => "Programado",
        _ => "Finalizado"
    };

    // UX-UI §5.3: Vigente en verde, Programado en azul, Finalizado neutro
    private static string TipoEstado(EstadoDescuento estado) => estado switch
    {
        EstadoDescuento.VIGENTE => "ok",
        EstadoDescuento.PROGRAMADO => "info",
        _ => "neutral"
    };
}
