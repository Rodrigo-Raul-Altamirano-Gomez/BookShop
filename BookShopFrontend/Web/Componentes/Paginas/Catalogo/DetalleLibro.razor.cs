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

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id { get; set; }
    private Libro? Libro;

    private string? correoCargado;
    private bool procesando;
    private int cantidad = 1, idAnterior, paginaResenas = 1;
    private bool sinopsisCompleta, fichaExpandida;
    private bool Deseado, Favorito;
    private List<ResenaDTO> resenas = [];
    private double Promedio => Libro?.Valoracion.Promedio ?? 0;
    private string EstiloPromedio => $"--relleno:{(Promedio * 20).ToString(CultureInfo.InvariantCulture)}%";
    private int PaginasResenas => Math.Max(1, (int)((totalResenas + 9) / 10));
    private List<Libro> LibrosSimilares = [];
    private DetalleLibroDTO? detalleLibro;
    private long totalResenas;

    private static readonly int[] Puntuaciones = [5, 4, 3, 2, 1];
    private static readonly CultureInfo CulturaEspanola = CultureInfo.GetCultureInfo("es-PE");
    protected override void OnInitialized() => Sesion.Cambiado += ActualizarVista;
    protected override async Task OnParametersSetAsync()
    {
        correoCargado = Sesion.Usuario?.Correo;
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
            Libro = Presentacion.Libro(detalle);
            Favorito = detalle.Favorito;
            Deseado = detalle.Deseado;
            var similares = await Api.Get<List<TarjetaLibroDTO>>($"recomendaciones/similares/{Id}");
            LibrosSimilares = similares.Select(Presentacion.Libro).ToList();
            await CargarResenas();
            idAnterior = Id;
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        cargando = false;
    }
    private async Task CargarResenas()
    {
        var datos = await Api.Post<ResultadoPaginadoDTO<ResenaDTO>>(
            $"resenas/libro/{Id}/busqueda", new ConsultaResenasDTO
            {
                Orden = "MAS_RECIENTES",
                Pagina = paginaResenas,
                TamanioPagina = 10
            });
        totalResenas = datos.TotalResultados;
        resenas = datos.Items;
    }
    private long CantidadEstrellas(int puntuacion) => puntuacion switch
    {
        1 => Libro?.Valoracion.CantidadUnaEstrella ?? 0,
        2 => Libro?.Valoracion.CantidadDosEstrellas ?? 0,
        3 => Libro?.Valoracion.CantidadTresEstrellas ?? 0,
        4 => Libro?.Valoracion.CantidadCuatroEstrellas ?? 0,
        _ => Libro?.Valoracion.CantidadCincoEstrellas ?? 0
    };

    private async void ActualizarVista()
    {
        await InvokeAsync(async () =>
        {
            if (correoCargado != Sesion.Usuario?.Correo)
            {
                idAnterior = 0;
                await OnParametersSetAsync();
            }
            StateHasChanged();
        });
    }
    public void Dispose() => Sesion.Cambiado -= ActualizarVista;
    private static string TextoEstrellas(int puntuacion) => new string('★', puntuacion) + new string('☆',
         5 - puntuacion);
    private int PorcentajePuntuacion(int puntuacion)
    {
        if (totalResenas == 0)
            return 0;
        return (int)(CantidadEstrellas(puntuacion) * 100 / totalResenas);
    }
    private void Disminuir()
    {
        if (cantidad > 1)
            cantidad--;
    }

    private void Aumentar()
    {
        if (cantidad < Libro?.Stock) cantidad++;
    }
    private void AlternarSinopsis() => sinopsisCompleta = !sinopsisCompleta;
    private void AlternarFicha() => fichaExpandida = !fichaExpandida;
    private async Task AgregarCarrito()
    {
        if (procesando) return;
        if (!Sesion.RequerirAcceso())
            return;
        procesando = true;
        try
        {
            await Compras.AgregarLibro(Id, cantidad);
            Sesion.Avisar("Libro agregado al carrito.");
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task AlternarGuardado(bool favorito)
    {
        if (procesando) return;
        if (!Sesion.RequerirAcceso())
            return;
        procesando = true;
        try
        {
            bool guardar = favorito ? !Favorito : !Deseado;
            await Cuenta.CambiarGuardado(Id, favorito, guardar);
            if (favorito)
                Favorito = guardar;
            else
                Deseado = guardar;
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private void Comparar()
    {
        if (Sesion.LibroComparacion?.Id == Id)
        {
            Sesion.Avisar("Selecciona otro libro para comparar");
            return;
        }

        if (Sesion.LibroComparacion is null)
        {
            Sesion.LibroComparacion = Libro;
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
        cargando = false;
    }
}
