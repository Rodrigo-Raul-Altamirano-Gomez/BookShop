using BookShopWeb.Code;
using BookShopWeb.Components.Shared;
using BookShopWeb.Models;
using BookShopWeb.Sesion;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Microsoft.JSInterop;

namespace BookShopWeb.Components.Pages.Admin;

// RF04.1–2 y UX-UI §10.4: autores, categorías y editoriales tienen la misma pantalla
public partial class AdminEntidades
{
    private const int TamanoPagina = 20;

    [Inject] private NavigationManager Navegacion { get; set; } = default!;
    [Inject] private IJSRuntime JS { get; set; } = default!;
    [Inject] private IWebHostEnvironment Entorno { get; set; } = default!;
    [Inject] private AvisoService Aviso { get; set; } = default!;

    // "autores", "categorias" o "editoriales"; al cambiar, Blazor vuelve a cargar la página
    [Parameter] public string Ruta { get; set; } = "";

    [SupplyParameterFromQuery] public string? Q { get; set; }
    [SupplyParameterFromQuery] public string? Estado { get; set; }
    [SupplyParameterFromQuery] public int? Pagina { get; set; }

    private TipoEntidad tipo;
    private int? registrados;
    private ResultadoPagina<EntidadAdmin>? resultado;
    private string busqueda = "";
    private string estado = "";
    private Entidad edicion = new();
    private Entidad? entidadElegida;
    private bool guardando;
    private ConfirmModal confirmacion = default!;

    private int TotalPaginas => Math.Max(1, (resultado!.Total + TamanoPagina - 1) / TamanoPagina);

    protected override async Task OnParametersSetAsync()
    {
        TipoEntidad nuevoTipo = Entidades.DesdeRuta(Ruta);
        if (nuevoTipo != tipo)
        {
            tipo = nuevoTipo;
            registrados = null;
            resultado = null;
        }
        busqueda = Q ?? "";
        estado = Estado ?? "";
        await Recargar();
    }

    private async Task Recargar()
    {
        string consulta = $"entidades/admin?tipo={tipo}&pagina={Pagina ?? 1}&tamano={TamanoPagina}";
        if (busqueda != "") consulta += "&q=" + Uri.EscapeDataString(busqueda);
        if (estado != "") consulta += "&estado=" + estado;
        // "{N} registrados" cuenta todos, sin filtros
        Task<ResultadoPagina<EntidadAdmin>?> pedidoTotal = new HttpClientUtils<ResultadoPagina<EntidadAdmin>>().Get($"entidades/admin?tipo={tipo}&tamano=1");
        Task<ResultadoPagina<EntidadAdmin>?> pedidoLista = new HttpClientUtils<ResultadoPagina<EntidadAdmin>>().Get(consulta);
        registrados = (await pedidoTotal)!.Total;
        resultado = await pedidoLista;
    }

    private void Filtrar()
    {
        IrA(1);
    }

    private void IrA(int pagina)
    {
        string url = "admin/" + Ruta + "?pagina=" + pagina;
        if (busqueda.Trim() != "") url += "&q=" + Uri.EscapeDataString(busqueda.Trim());
        if (estado != "") url += "&estado=" + estado;
        Navegacion.NavigateTo(url);
    }

    // "1–20 de 36"
    private string Rango()
    {
        int desde = ((Pagina ?? 1) - 1) * TamanoPagina + 1;
        int hasta = desde + resultado!.Elementos.Count - 1;
        return $"{desde}–{hasta} de {resultado.Total}";
    }

    // Sin entidad, el alta; con una entidad, su edición
    private async Task AbrirDialogo(Entidad? existente)
    {
        edicion = new Entidad { Tipo = tipo, Activo = true };
        if (existente != null)
        {
            edicion.Id = existente.Id;
            edicion.Nombre = existente.Nombre;
            edicion.Descripcion = existente.Descripcion;
            edicion.Imagen = existente.Imagen;
            edicion.Activo = existente.Activo;
        }
        await JS.InvokeVoidAsync("abrirModal", "m-entidad");
        await JS.InvokeVoidAsync("enfocar", "#ent-n");
    }

    private async Task ElegirImagen(InputFileChangeEventArgs e)
    {
        edicion.Imagen = await Imagenes.Guardar(e.File, Entorno.WebRootPath);
    }

    private async Task Guardar()
    {
        guardando = true;
        if (edicion.Id == 0)
        {
            await new HttpClientUtils<Entidad>().Post("entidades", edicion);
        }
        else
        {
            await new HttpClientUtils<object>().Put($"entidades/{edicion.Id}", edicion);
        }
        await JS.InvokeVoidAsync("cerrarModal", "m-entidad");
        guardando = false;
        Aviso.Mostrar("Cambios guardados");
        await Recargar();
    }

    private async Task PedirCambioEstado(Entidad entidad)
    {
        entidadElegida = entidad;
        if (entidad.Activo)
        {
            await confirmacion.Mostrar($"¿Desactivar «{entidad.Nombre}»?", "Dejará de mostrarse en la tienda. Lo ya registrado (pedidos, listas, reseñas) no cambia.", "Sí, desactivar", true);
        }
        else
        {
            await confirmacion.Mostrar($"¿Activar «{entidad.Nombre}»?", "Volverá a mostrarse en la tienda.", "Sí, activar", false);
        }
    }

    // RF04.2 y RN03: se activa o desactiva, nunca se borra
    private async Task CambiarEstado()
    {
        string accion = entidadElegida!.Activo ? "desactivar" : "activar";
        await new HttpClientUtils<object>().Put($"entidades/{entidadElegida.Id}/{accion}", new { });
        string genero = Entidades.Genero(tipo);
        Aviso.Mostrar($"«{entidadElegida.Nombre}» {(entidadElegida.Activo ? "desactivad" : "activad")}{genero}");
        await Recargar();
    }
}
