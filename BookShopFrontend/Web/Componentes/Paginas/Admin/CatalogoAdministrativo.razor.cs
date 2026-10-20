using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Admin;

public partial class CatalogoAdministrativo : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = null!;

    private string Tipo = "libros", Orden = "Nombre", ErrorFormulario = "";
    private bool BorradorSoloActivos = true, SoloActivos = true, MostrarReferencia, MostrarLibro;
    private int Pagina = 1;
    private const int TamanoPagina = 12;
    private string NombreReferencia = "", DescripcionReferencia = "", ImagenReferencia =
         "/images/reference-placeholder.svg";
    private string Titulo => Tipo switch
    {
        "autores" => "Autores",
        "categorias" => "Categorías",
        "editoriales" => "Editoriales",
        _ => "Libros"
    };
    private string Singular => Tipo switch
    {
        "autores" => "autor",
        "categorias" => "categoría",
        "editoriales" => "editorial",
        _ => "libro"
    };

    private IEnumerable<Referencia> Filtrados
    {
        get
        {
            var Resultado = Estado.Referencias.Where(r => r.Tipo == Tipo && (!SoloActivos || r.Activo));
            return Orden == "recent" ? Resultado.OrderByDescending(r => r.Id) : Resultado.OrderBy(r => r
                .Nombre);
        }
    }

    private IEnumerable<Referencia> ElementosPagina => Filtrados.Skip((Pagina - 1) * TamanoPagina).Take(
        TamanoPagina);
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Filtrados.Count() / (double)TamanoPagina));

    protected override void OnInitialized() => Estado.Cambiado += ActualizarVista;
    protected override void OnParametersSet()
    {
        var SiguienteTipo = Navegacion.ToBaseRelativePath(Navegacion.Uri).Split('?')[0].Split('/').Last();
        if (Tipo != SiguienteTipo)
        {
            Tipo = SiguienteTipo;
            Pagina = 1;
            BorradorSoloActivos = SoloActivos = true;
            Orden = "Nombre";
            MostrarLibro = MostrarReferencia = false;
        }
    }

    private void ActualizarVista() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private void Aplicar()
    {
        SoloActivos = BorradorSoloActivos;
        Pagina = 1;
    }

    private void Limpiar()
    {
        SoloActivos = BorradorSoloActivos = false;
        Pagina = 1;
    }

    private void QuitarSoloActivos()
    {
        SoloActivos = BorradorSoloActivos = false;
        Pagina = 1;
    }

    private void ReiniciarPagina() => Pagina = 1;
    private void CambiarPagina(int Siguiente) => Pagina = Siguiente;
    private void AbrirAlta()
    {
        if (Tipo == "libros")
        {
            MostrarLibro = true;
            return;
        }

        NombreReferencia = DescripcionReferencia = ErrorFormulario = "";
        ImagenReferencia = "/images/reference-placeholder.svg";
        MostrarReferencia = true;
    }

    private void CerrarLibro() => MostrarLibro = false;
    private void CerrarReferencia() => MostrarReferencia = false;
    private void QuitarImagenReferencia() => ImagenReferencia = "/images/reference-placeholder.svg";
    private async Task LeerImagenReferencia(InputFileChangeEventArgs Argumentos)
    {
        ErrorFormulario = "";
        if (Argumentos.File.ContentType is not ("image/jpeg" or "image/png") || Argumentos.File
            .Size > 5 * 1024 * 1024)
        {
            ErrorFormulario = "Selecciona una imagen JPEG o PNG de hasta 5 MiB.";
            return;
        }

        try
        {
            using var Flujo = Argumentos.File.OpenReadStream(5 * 1024 * 1024);
            using var Memoria = new MemoryStream();
            await Flujo.CopyToAsync(Memoria);
            ImagenReferencia = ($"data:" + $"{Argumentos.File.ContentType};base64," + $"{Convert
                .ToBase64String(Memoria.ToArray())}");
        }
        catch (IOException)
        {
            ErrorFormulario = "No se pudo leer la imagen. Prueba con otro archivo.";
        }
    }

    private void AgregarReferencia()
    {
        if (string.IsNullOrWhiteSpace(NombreReferencia) || string.IsNullOrWhiteSpace(DescripcionReferencia))
        {
            ErrorFormulario = "Completa los campos obligatorios.";
            return;
        }

        Estado.Referencias.Add(new Referencia
        {
            Id = Estado.Referencias.Where(r => r.Tipo == Tipo).Select(r => r.Id).DefaultIfEmpty(0).Max() + 1,
            Tipo = Tipo,
            Nombre = NombreReferencia.Trim(),
            Descripcion = DescripcionReferencia.Trim(),
            Imagen = ImagenReferencia
        });
        MostrarReferencia = false;
        Pagina = 1;
        Estado.Notificar();
        Estado.Avisar("Referencia añadida");
    }
}

