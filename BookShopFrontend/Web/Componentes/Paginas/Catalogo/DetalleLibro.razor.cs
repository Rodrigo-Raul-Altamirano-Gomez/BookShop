using System.Globalization;
using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleLibro : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CompraServicio Compras { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id
    {
        get; set;
    }
    private Libro? Libro => Catalogo.Libros.FirstOrDefault(libro => libro.Id == Id);

    private int cantidad = 1, idAnterior, paginaResenas = 1;
    private bool sinopsisCompleta, fichaExpandida;
    private bool Deseado => Cuenta.Deseados.Contains(Id);
    private bool Favorito => Cuenta.Favoritos.Contains(Id);
    private List<Resena> Resenas => Catalogo.Resenas.Where(r => r.IdLibro == Id && r.Activo)
        .OrderByDescending(r => r.Fecha).ToList();
    private double Promedio => totalResenas == 0 ? 0 : Enumerable.Range(1, 5).Sum(n => n * CantidadEstrellas(n)) / (double)totalResenas;
    private string EstiloPromedio => $"--relleno:{(Promedio * 20).ToString(CultureInfo.InvariantCulture)}%";
    private IEnumerable<Resena> ResenasVisibles => Resenas;
    private int PaginasResenas => Math.Max(1, (int)((totalResenas + 9) / 10));
    private List<Libro> LibrosSimilares = [];
    private ValoracionLibroDTO? valoracion;
    private DetalleLibroDTO? detalleLibro;
    private long totalResenas;

    private static readonly int[] Puntuaciones = [5, 4, 3, 2, 1];
    private static readonly CultureInfo CulturaEspanola = CultureInfo.GetCultureInfo("es-PE");
    protected override void OnInitialized() => Sesion.Cambiado += ActualizarVista;
    protected override async Task OnParametersSetAsync()
    {
        if (idAnterior == Id)
            return;
        cargando = true;
        errorCarga = null;
        try
        {

            if (idAnterior == Id)
                return;
            idAnterior = Id;
            cantidad = paginaResenas = 1;
            sinopsisCompleta = fichaExpandida = false;

            var detalle = await Api.Get<DetalleLibroDTO>($"libros/{Id}");
            detalleLibro = detalle;
            Catalogo.Recordar(Presentacion.Libro(detalle));
            valoracion = detalle.Valoracion;
            if (detalle.Favorito)
                Cuenta.Favoritos.Add(Id);
            else
                Cuenta.Favoritos.Remove(Id);
            if (detalle.Deseado)
                Cuenta.Deseados.Add(Id);
            else
                Cuenta.Deseados.Remove(Id);
            LibrosSimilares = (await Api.Get<List<TarjetaLibroDTO>>($"recomendaciones/similares/{Id}")).Select(Presentacion.Libro).ToList();
            await CargarResenas();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }
    private async Task CargarResenas()
    {
        var datos = await Api.Post<ResultadoPaginadoDTO<ResenaDTO>>($"resenas/libro/{Id}/busqueda", new ConsultaResenasDTO
        {
            Orden = "MAS_RECIENTES",
            Pagina = paginaResenas,
            TamanioPagina = 10
        });
        totalResenas = datos.TotalResultados;
        Catalogo.Resenas.RemoveAll(r => r.IdLibro == Id);
        Catalogo.Resenas.AddRange(datos.Items.Select(d => new Resena
        {
            Id = d.Id,
            IdLibro = d.IdLibro,
            Usuario = d.NombreUsuario,
            FotoUsuario = Presentacion.Imagen(d.ImagenUsuario, "/images/user-placeholder.svg"),
            Fecha = d.Fecha,
            Puntuacion = d.Puntuacion,
            Comentario = d.Comentario,
            Votos = d.CantidadVotosUtiles,
            Activo = d.Activo
        }));
    }
    private long CantidadEstrellas(int puntuacion) => puntuacion switch
    {
        1 => valoracion?.CantidadUnaEstrella ?? 0,
        2 => valoracion?.CantidadDosEstrellas ?? 0,
        3 => valoracion?.CantidadTresEstrellas ?? 0,
        4 => valoracion?.CantidadCuatroEstrellas ?? 0,
        _ => valoracion?.CantidadCincoEstrellas ?? 0
    };

    private void ActualizarVista() => InvokeAsync(StateHasChanged);
    public void Dispose() => Sesion.Cambiado -= ActualizarVista;
    private string RutaReferencia(string kind, string name) => $"/referencias/{kind}/{(kind == "autores" ? detalleLibro?.IdAutor : kind == "categorias" ? detalleLibro?.IdCategoria : detalleLibro?.IdEditorial)}";
    private static string TextoEstrellas(int rating) => new string('★', rating) + new string('☆',
         5 - rating);
    private int PorcentajePuntuacion(int rating) => totalResenas == 0 ? 0 : (int)(CantidadEstrellas(rating) * 100 / totalResenas);
    private void Disminuir()
    {
        if (cantidad > 1)
            cantidad--;
    }

    private void Aumentar() => cantidad++;
    private void AlternarSinopsis() => sinopsisCompleta = !sinopsisCompleta;
    private void AlternarFicha() => fichaExpandida = !fichaExpandida;
    private async Task AgregarCarrito()
    {
        if (!Sesion.RequerirAcceso())
            return;
        try
        {
            await Compras.AgregarLibro(Id, cantidad);
            Sesion.Avisar("Libro agregado al carrito.");
        }
        catch (InvalidOperationException e) { Sesion.Avisar(e.Message); }
    }

    private async Task AlternarDeseado()
    {
        try
        {
            await Cuenta.AlternarGuardado(Id, false);
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private async Task AlternarFavorito()
    {
        try
        {
            await Cuenta.AlternarGuardado(Id, true);
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

    private void Comparar()
    {
        if (Sesion.IdComparacion == Id)
        {
            Sesion.Avisar("Selecciona otro libro para comparar");
            return;
        }

        if (Sesion.IdComparacion is null)
        {
            Sesion.IdComparacion = Id;
            Sesion.Notificar();
            Navegacion.NavigateTo("/catalogo");
        }
        else
            Navegacion.NavigateTo($"/comparacion?con={Id}");
    }

    private async Task CambiarPaginaResenas(int pagina)
    {
        cargando = true;
        errorCarga = null;
        try
        {

            paginaResenas = pagina;

            await CargarResenas();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }
}
