using System.Globalization;
using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleLibro : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id { get; set; }
    private Libro? Libro => Catalogo.Libros.FirstOrDefault(libro => libro.Id == Id);

    private int cantidad = 1, idAnterior, paginaResenas = 1;
    private bool sinopsisCompleta, fichaExpandida;
    private bool Deseado => Cuenta.Deseados.Contains(Id);
    private bool Favorito => Cuenta.Favoritos.Contains(Id);
    private List<Resena> resenas = [];
    private double Promedio
    {
        get
        {
            if (totalResenas == 0) return 0;
            long puntos = 0;
            for (int estrellas = 1; estrellas <= 5; estrellas++)
                puntos += estrellas * CantidadEstrellas(estrellas);
            return puntos / (double)totalResenas;
        }
    }
    private string EstiloPromedio => $"--relleno:{(Promedio * 20).ToString(CultureInfo.InvariantCulture)}%";
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
            idAnterior = Id;
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
        resenas = datos.Items.Where(dto => dto.Activo).Select(dto => new Resena
        {
            Id = dto.Id,
            IdLibro = dto.IdLibro,
            Usuario = dto.NombreUsuario,
            FotoUsuario = Presentacion.Imagen(dto.ImagenUsuario, "/images/user-placeholder.svg"),
            Fecha = dto.Fecha,
            Puntuacion = dto.Puntuacion,
            Comentario = dto.Comentario,
            Votos = dto.CantidadVotosUtiles,
            Activo = dto.Activo
        }).ToList();
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
    private static string TextoEstrellas(int puntuacion) => new string('★', puntuacion) + new string('☆',
         5 - puntuacion);
    private int PorcentajePuntuacion(int puntuacion) => totalResenas == 0 ? 0 : (int)(CantidadEstrellas(puntuacion) * 100 / totalResenas);
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
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
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
            idAnterior = Id;
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
