using System.Globalization;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleLibro : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    [Parameter]
    public int Id
    {
        get; set;
    }
    private Libro? Libro => Estado.BuscarLibro(Id);

    private int cantidad = 1, idAnterior, paginaResenas = 1, puntuacionResena;
    private bool sinopsisCompleta, fichaExpandida, modalResena, modalReporte, cargandoImagen;
    private string comentarioResena = "", motivoReporte = "", comentarioReporte = "", errorFormulario = "";
    private Resena? resenaReportada;
    private bool Deseado => Estado.Deseados.Contains(Id);
    private bool Favorito => Estado.Favoritos.Contains(Id);
    private List<Resena> Resenas => Estado.Resenas.Where(r => r.IdLibro == Id && r.Activo)
        .OrderByDescending(r => r.Fecha).ToList();
    private double Promedio => Resenas.Count == 0 ? 0 : Resenas.Average(r => r.Puntuacion);
    private string EstiloPromedio => $"--relleno:{(Promedio * 20).ToString(CultureInfo.InvariantCulture)}%";
    private IEnumerable<Resena> ResenasVisibles => Resenas.Skip((paginaResenas - 1) * 10).Take(10);
    private int PaginasResenas => Math.Max(1, (Resenas.Count + 9) / 10);
    private IEnumerable<Libro> LibrosSimilares => Estado.Libros.Where(b => b.Id != Id && b.Activo && b
        .Categoria == Libro?.Categoria).Take(8);

    private static readonly int[] Puntuaciones = [5, 4, 3, 2, 1], PuntuacionesAscendentes = [1, 2, 3, 4, 5];
    private static readonly CultureInfo CulturaEspanola = CultureInfo.GetCultureInfo("es-PE");
    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    protected override void OnParametersSet()
    {
        if (idAnterior == Id)
            return;
        idAnterior = Id;
        cantidad = paginaResenas = 1;
        sinopsisCompleta = fichaExpandida = modalResena = modalReporte = false;
    }

    private void ActualizarVista() => InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private string RutaReferencia(string kind, string name) => ($"/referencias/" + $"{kind}/" + $"{Estado
        .Referencias.FirstOrDefault(r => r.Tipo == kind && r.Nombre == name)?.Id ?? 0}");
    private static string TextoEstrellas(int rating) => new string('★', rating) + new string('☆',
         5 - rating);
    private int PorcentajePuntuacion(int rating) => Resenas.Count == 0 ? 0 : Resenas.Count(r => r
        .Puntuacion == rating) * 100 / Resenas.Count;
    private void Disminuir()
    {
        if (cantidad > 1)
            cantidad--;
    }

    private void Aumentar() => cantidad++;
    private void AlternarSinopsis() => sinopsisCompleta = !sinopsisCompleta;
    private void AlternarFicha() => fichaExpandida = !fichaExpandida;
    private void AgregarCarrito()
    {
        Estado.Carrito[Id] = Estado.Carrito.GetValueOrDefault(Id) + cantidad;
        Estado.Notificar();
        Estado.Avisar($"{cantidad} {(cantidad == 1 ? "libro agregado" : "libros agregados")} al carrito");
    }

    private void AlternarDeseado()
    {
        if (!Estado.RequerirAcceso())
            return;
        if (!Estado.Deseados.Add(Id))
            Estado.Deseados.Remove(Id);
        Estado.Avisar(Deseado ? "Libro añadido a deseados" : "Libro retirado de deseados");
        Estado.Notificar();
    }

    private void AlternarFavorito()
    {
        if (!Estado.RequerirAcceso())
            return;
        if (!Estado.Favoritos.Add(Id))
            Estado.Favoritos.Remove(Id);
        Estado.Avisar(Favorito ? "Libro añadido a favoritos" : "Libro retirado de favoritos");
        Estado.Notificar();
    }

    private void Comparar()
    {
        if (Estado.IdComparacion == Id)
        {
            Estado.Avisar("Selecciona otro libro para comparar");
            return;
        }

        if (Estado.IdComparacion is null)
        {
            Estado.IdComparacion = Id;
            Estado.Notificar();
            Navegacion.NavigateTo("/catalogo");
        }
        else
            Navegacion.NavigateTo($"/comparacion?con={Id}");
    }

    private void Votar(Resena review)
    {
        if (!Estado.RequerirAcceso())
            return;
        review.Votada = !review.Votada;
        review.Votos += review.Votada ? 1 : -1;
        Estado.Notificar();
    }

    private void AbrirResena()
    {
        if (!Estado.RequerirAcceso())
            return;
        if (Resenas.Any(r => r.Propia))
        {
            Estado.Avisar("Ya has publicado una reseña de este libro");
            return;
        }

        puntuacionResena = 0;
        comentarioResena = errorFormulario = "";
        modalResena = true;
    }

    private void CerrarResena() => modalResena = false;
    private void SeleccionarPuntuacion(int rating) => puntuacionResena = rating;
    private void PublicarResena()
    {
        if (puntuacionResena == 0 || string.IsNullOrWhiteSpace(comentarioResena))
        {
            errorFormulario = "Selecciona una puntuación y escribe tu comentario";
            return;
        }

        Estado.Resenas.Add(new Resena
        {
            Id = Estado.Resenas.Select(r => r.Id).DefaultIfEmpty(0).Max() + 1,
            IdLibro = Id,
            Usuario = Estado.Usuario!.Nombre,
            FotoUsuario = Estado.Usuario.Imagen,
            Fecha = new DateTime(2026, 10, 7, 12, 0, 0),
            Puntuacion = puntuacionResena,
            Comentario = comentarioResena.Trim(),
            Propia = true
        });
        modalResena = false;
        paginaResenas = 1;
        Estado.Avisar("Reseña publicada en esta demostración");
        Estado.Notificar();
    }

    private void AbrirReporte(Resena review)
    {
        if (!Estado.RequerirAcceso())
            return;
        if (Estado.Solicitudes.Any(r => r.Tipo == "Reportes" && r.IdReferencia == review.Id && r
            .Estado == "Pendiente" && r.Usuario == Estado.Usuario!.Nombre))
        {
            Estado.Avisar("Ya tienes un reporte pendiente para esta reseña");
            return;
        }

        resenaReportada = review;
        motivoReporte = comentarioReporte = errorFormulario = "";
        modalReporte = true;
    }

    private void CerrarReporte() => modalReporte = false;
    private void EnviarReporte()
    {
        if (motivoReporte.Length == 0 || resenaReportada is null)
        {
            errorFormulario = "Selecciona un motivo";
            return;
        }

        Estado.Solicitudes.Add(new Solicitud
        {
            Id = Estado.Solicitudes.Select(r => r.Id).DefaultIfEmpty(0).Max() + 1,
            Tipo = "Reportes",
            IdReferencia = resenaReportada.Id,
            Usuario = Estado.Usuario!.Nombre,
            FechaCreacion = new DateTime(2026, 10, 7, 12, 0, 0),
            Motivo = motivoReporte,
            Comentario = comentarioReporte.Trim(),
            ComentarioOriginal = resenaReportada.Comentario,
            PuntuacionOriginal = resenaReportada.Puntuacion
        });
        modalReporte = false;
        Estado.Avisar("Reporte enviado en esta demostración");
        Estado.Notificar();
    }

    private void CambiarPaginaResenas(int pagina) => paginaResenas = pagina;
    private async Task CambiarImagen(InputFileChangeEventArgs args)
    {
        if (Libro is null || !Estado.EsAdministrador || cargandoImagen)
            return;
        var file = args.File;
        if (file.Size > 5 * 1024 * 1024 || (file.ContentType != "image/png" && file
            .ContentType != "image/jpeg"))
        {
            Estado.Avisar("Selecciona una imagen JPEG o PNG de hasta 5 MiB");
            return;
        }

        cargandoImagen = true;
        try
        {
            using var stream = new MemoryStream();
            await file.OpenReadStream(5 * 1024 * 1024).CopyToAsync(stream);
            Libro.Portada = $"data:{file.ContentType};base64,{Convert.ToBase64String(stream.ToArray())}";
            Estado.Avisar("Portada actualizada en esta demostración");
            Estado.Notificar();
        }
        catch
        {
            Estado.Avisar("No se pudo cargar la portada. Inténtalo de nuevo");
        }
        finally
        {
            cargandoImagen = false;
        }
    }

    private void QuitarImagen()
    {
        if (Libro is null || !Estado.EsAdministrador)
            return;
        Libro.Portada = "/images/book-placeholder.svg";
        Estado.Avisar("Portada retirada");
        Estado.Notificar();
    }
}

