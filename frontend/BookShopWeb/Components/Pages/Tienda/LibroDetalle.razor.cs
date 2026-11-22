using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Security;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Tienda;

// RF09, RF06.1, RF10 y RF12 (UX-UI §8.3)
public partial class LibroDetalle
{
    [Parameter] public int Id { get; set; }
    [Inject] private ComparacionService Comparacion { get; set; } = default!;
    [Inject] private CarritoService Carrito { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;
    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [CascadingParameter] private Task<AuthenticationState> AuthState { get; set; } = default!;

    private Libro? libro;
    private bool noEncontrado;
    private int idUsuario;          // 0: visitante
    private bool esAdmin;
    private int cantidad = 1;
    private List<TipoLista> listas = new();
    private TipoLista? cambiandoLista;
    private ResenasLibro? resenas;
    private List<Libro>? similares;

    // Reseñas: se ven 5 (más la propia) y "Ver más" agrega 5 (UX-UI §8.3)
    private int visibles = 5;
    private Resena? editando;
    private int puntuacion = 5;
    private string comentario = "";
    private bool guardandoResena;
    private int votando;
    private int moderando;
    private Resena? reportada;
    private MotivoReporte motivoReporte;
    private string comentarioReporte = "";
    private bool enviandoReporte;
    private ConfirmModal confirmacion = default!;

    // Al pasar de un libro a otro (enlaces del detalle o de Similares), se vuelve a cargar
    protected override async Task OnParametersSetAsync()
    {
        var usuario = (await AuthState).User;
        idUsuario = usuario.Identity?.IsAuthenticated == true ? SesionUsuario.Id(usuario) : 0;
        esAdmin = SesionUsuario.EsAdmin(usuario);
        libro = null;
        resenas = null;
        similares = null;
        noEncontrado = false;
        cantidad = 1;
        visibles = 5;
        editando = null;
        try
        {
            libro = await new HttpClientUtils<Libro>().Get($"libros/{Id}");
        }
        catch (ApiException ex) when (ex.Estado == 404)
        {
            noEncontrado = true;
            return;
        }
        Task<ResenasLibro?> pedidoResenas = new HttpClientUtils<ResenasLibro>().Get($"resenas/libro/{Id}?usuario={idUsuario}&ocultas={esAdmin}");
        Task<ResultadoLibros?> pedidoSimilares = new HttpClientUtils<ResultadoLibros>().Get($"libros/{Id}/similares?tamano=8");
        if (idUsuario > 0 && !esAdmin)
        {
            listas = (await new HttpClientUtils<List<TipoLista>>().Get($"listas/usuario/{idUsuario}/libro/{Id}"))!;
        }
        resenas = await pedidoResenas;
        similares = (await pedidoSimilares)!.Libros;
    }

    // Tras publicar, editar, eliminar u ocultar una reseña cambian la lista y el promedio del libro
    private async Task RecargarResenas()
    {
        Task<Libro?> pedidoLibro = new HttpClientUtils<Libro>().Get($"libros/{Id}");
        resenas = await new HttpClientUtils<ResenasLibro>().Get($"resenas/libro/{Id}?usuario={idUsuario}&ocultas={esAdmin}");
        libro = await pedidoLibro;
    }

    // Las acciones que requieren cuenta llevan al visitante a iniciar sesión (UX-UI §3.3)
    private bool PedirSesion()
    {
        if (idUsuario == 0)
        {
            Navegacion.NavigateTo("login?aviso=1&volver=" + Uri.EscapeDataString("/libros/" + Id));
            return true;
        }
        return false;
    }

    // RF09.5 y RF11.2: si ya está en el carrito, se suma la cantidad
    private async Task AgregarAlCarrito()
    {
        if (PedirSesion()) return;
        await Carrito.Agregar(libro!.Id, cantidad);
        Aviso.Mostrar("Agregado al carrito.", "Ver carrito", "carrito");
        cantidad = 1;
    }

    // RF06.1: agrega o quita el libro de la lista
    private async Task AlternarLista(TipoLista lista)
    {
        if (PedirSesion()) return;
        cambiandoLista = lista;
        string ruta = $"listas/{lista}/usuario/{idUsuario}/libro/{Id}";
        if (listas.Contains(lista))
        {
            await new HttpClientUtils<object>().Delete(ruta);
            listas.Remove(lista);
        }
        else
        {
            await new HttpClientUtils<object>().Put(ruta, new { });
            listas.Add(lista);
        }
        cambiandoLista = null;
    }

    // RF09.7: cualquier usuario puede comparar
    private async Task Comparar()
    {
        await Comparacion.Agregar(libro!);
    }

    // La reseña propia va siempre arriba y no cuenta entre las visibles
    private List<Resena> ResenasVisibles()
    {
        List<Resena> lista = new();
        int otras = 0;
        foreach (Resena resena in resenas!.Resenas)
        {
            if (resena.Usuario.Id == idUsuario)
            {
                lista.Add(resena);
            }
            else if (otras < visibles)
            {
                lista.Add(resena);
                otras++;
            }
        }
        return lista;
    }

    private int ResenasRestantes()
    {
        int otras = resenas!.Resenas.Count(r => r.Usuario.Id != idUsuario);
        return Math.Max(0, otras - visibles);
    }

    private async Task EditarResena(Resena resena)
    {
        editando = resena;
        puntuacion = resena.Puntuacion;
        comentario = resena.Comentario;
        await JS.InvokeVoidAsync("enfocar", "#c-res");
    }

    // RF10.1–2: publica la reseña o guarda los cambios de la propia
    private async Task GuardarResena()
    {
        guardandoResena = true;
        if (editando != null)
        {
            await new HttpClientUtils<object>().Put($"resenas/{editando.Id}", new Resena { Puntuacion = puntuacion, Comentario = comentario.Trim() });
            Aviso.Mostrar("Cambios guardados");
        }
        else
        {
            Resena nueva = new Resena
            {
                Usuario = new Usuario { Id = idUsuario },
                Libro = new Libro { Id = Id },
                Puntuacion = puntuacion,
                Comentario = comentario.Trim()
            };
            await new HttpClientUtils<Resena>().Post("resenas", nueva);
            Aviso.Mostrar("Reseña publicada.");
        }
        editando = null;
        comentario = "";
        puntuacion = 5;
        await RecargarResenas();
        guardandoResena = false;
    }

    private async Task PedirEliminarResena()
    {
        await confirmacion.Mostrar();
    }

    private async Task EliminarResena()
    {
        Resena propia = resenas!.Resenas.First(r => r.Usuario.Id == idUsuario);
        await new HttpClientUtils<object>().Delete($"resenas/{propia.Id}");
        Aviso.Mostrar("Reseña eliminada");
        await RecargarResenas();
    }

    // RF10.5: un voto por reseña; volver a pulsar lo retira
    private async Task AlternarVoto(Resena resena)
    {
        if (PedirSesion()) return;
        votando = resena.Id;
        await new HttpClientUtils<object>().Put($"resenas/{resena.Id}/util/usuario/{idUsuario}", new { });
        resena.VotosUtiles += resena.VotadaPorMi ? -1 : 1;
        resena.VotadaPorMi = !resena.VotadaPorMi;
        votando = 0;
    }

    private async Task AbrirReporte(Resena resena)
    {
        if (PedirSesion()) return;
        reportada = resena;
        motivoReporte = MotivoReporte.OFENSIVA;
        comentarioReporte = "";
        await JS.InvokeVoidAsync("abrirModal", "m-reportar");
    }

    // RF10.6–8: si ya tiene un reporte pendiente sobre esta reseña, se le avisa
    private async Task EnviarReporte()
    {
        enviandoReporte = true;
        ReporteResena reporte = new ReporteResena
        {
            Usuario = new Usuario { Id = idUsuario },
            Resena = new Resena { Id = reportada!.Id },
            Motivo = motivoReporte,
            Comentario = comentarioReporte.Trim()
        };
        try
        {
            await new HttpClientUtils<ReporteResena>().Post("reportes-resenas", reporte);
            Aviso.Mostrar("Gracias, revisaremos esta reseña.");
        }
        catch (ApiException ex) when (ex.Estado == 422)
        {
            Aviso.Mostrar(ex.Message);
        }
        await JS.InvokeVoidAsync("cerrarModal", "m-reportar");
        enviandoReporte = false;
    }

    // RF04.9: el administrador oculta o vuelve a mostrar cualquier reseña
    private async Task CambiarVisibilidad(Resena resena)
    {
        moderando = resena.Id;
        string accion = resena.Activa ? "ocultar" : "mostrar";
        await new HttpClientUtils<object>().Put($"resenas/{resena.Id}/{accion}", new { });
        Aviso.Mostrar(resena.Activa ? "Reseña ocultada" : "Reseña visible de nuevo");
        await RecargarResenas();
        moderando = 0;
    }
}
