using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleReferencia : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";

    [Parameter]
    public int Id
    {
        get; set;
    }

    private Referencia? Referencia => Catalogo.Referencias.FirstOrDefault(r => r.Tipo == Tipo && r.Id == Id);
    private string Titulo => Tipo == "autores" ? "Autores" : Tipo == "editoriales" ? "Editoriales"
        : "Categorías";
    private string Singular => Tipo == "autores" ? "autor" : Tipo == "editoriales" ? "editorial" : "categoría";
    private bool Seguimientos => Cuenta.Seguimientos.Contains($"{Tipo}:{Id}");

    protected override void OnInitialized() => Sesion.Cambiado += ActualizarVista;
    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        errorCarga = null;
        try
        {


            var d = await Api.Get<DetalleReferenciaDTO>($"{Tipo}/{Id}");
            var referencia = Presentacion.Referencia(d.Referencia, Tipo);
            referencia.Descripcion = d.Introduccion;
            referencia.Activo = d.Activo;
            Catalogo.Referencias.RemoveAll(r => r.Tipo == Tipo && r.Id == Id);
            Catalogo.Referencias.Add(referencia);
            if (d.Seguido)
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
