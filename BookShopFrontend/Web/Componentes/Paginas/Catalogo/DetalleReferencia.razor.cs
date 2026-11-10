using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleReferencia : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";

    [Parameter]
    public int Id { get; set; }

    private Referencia? Referencia => Catalogo.Referencias.FirstOrDefault(r => r.Tipo == Tipo && r.Id == Id);
    private string Titulo
    {
        get
        {
            if (Tipo == "autores") return "Autores";
            if (Tipo == "editoriales") return "Editoriales";
            return "Categorías";
        }
    }
    private string Singular
    {
        get
        {
            if (Tipo == "autores") return "autor";
            if (Tipo == "editoriales") return "editorial";
            return "categoría";
        }
    }
    private bool Seguimientos => Cuenta.Seguimientos.Contains($"{Tipo}:{Id}");

    protected override void OnInitialized() => Sesion.Cambiado += ActualizarVista;
    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {
            var dto = await Api.Get<DetalleReferenciaDTO>($"{Tipo}/{Id}");
            var referencia = Presentacion.Referencia(dto.Referencia, Tipo);
            referencia.Descripcion = dto.Introduccion;
            referencia.Activo = dto.Activo;
            Catalogo.Referencias.RemoveAll(r => r.Tipo == Tipo && r.Id == Id);
            Catalogo.Referencias.Add(referencia);
            if (dto.Seguido)
                Cuenta.Seguimientos.Add($"{Tipo}:{Id}");
            else
                Cuenta.Seguimientos.Remove($"{Tipo}:{Id}");
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
    private void ActualizarVista() => InvokeAsync(StateHasChanged);
    public void Dispose() => Sesion.Cambiado -= ActualizarVista;
    private async Task AlternarSeguimiento()
    {
        try
        {
            await Cuenta.AlternarSeguimiento(Tipo, Id);
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Error = e.Message;
            Sesion.Avisar(e.Message);
        }
    }

}
