using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleReferencia : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";

    [Parameter]
    public int Id
    {
        get; set;
    }

    private bool cargandoImagen;
    private Referencia? Referencia => Estado.Referencias.FirstOrDefault(r => r.Tipo == Tipo && r.Id == Id);
    private string Titulo => Tipo == "autores" ? "Autores" : Tipo == "editoriales" ? "Editoriales"
        : "Categorías";
    private string Singular => Tipo == "autores" ? "autor" : Tipo == "editoriales" ? "editorial" : "categoría";
    private bool Seguimientos => Estado.Seguimientos.Contains($"{Tipo}:{Id}");
    private IEnumerable<Libro> Libros => Estado.Libros.Where(b => Tipo == "autores" ? b.Autor == Referencia
        ?.Nombre : Tipo == "editoriales" ? b.Editorial == Referencia?.Nombre : b.Categoria == Referencia?
            .Nombre);

    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    private void ActualizarVista() => InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private void AlternarSeguimiento()
    {
        if (!Estado.RequerirAcceso())
            return;
        var clave = $"{Tipo}:{Id}";
        if (Estado.Seguimientos.Add(clave))
        {
            Estado.OrdenSeguimientos[clave] = Estado.OrdenSeguimientos.Values.DefaultIfEmpty().Max() + 1;
        }
        else
        {
            Estado.Seguimientos.Remove(clave);
            Estado.OrdenSeguimientos.Remove(clave);
        }

        Estado.Avisar(Seguimientos ? "Referencia añadida a tus seguimientos"
            : "Has dejado de seguir esta referencia");
        Estado.Notificar();
    }

    private async Task CambiarImagen(InputFileChangeEventArgs args)
    {
        if (Referencia is null || !Estado.EsAdministrador || cargandoImagen)
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
            Referencia.Imagen = $"data:{file.ContentType};base64,{Convert.ToBase64String(stream.ToArray())}";
            Estado.Avisar("Imagen actualizada en esta demostración");
            Estado.Notificar();
        }
        catch
        {
            Estado.Avisar("No se pudo cargar la imagen. Inténtalo de nuevo");
        }
        finally
        {
            cargandoImagen = false;
        }
    }

    private void QuitarImagen()
    {
        if (Referencia is null || !Estado.EsAdministrador)
            return;
        Referencia.Imagen = "/images/reference-placeholder.svg";
        Estado.Avisar("Imagen retirada");
        Estado.Notificar();
    }
}

