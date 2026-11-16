using Microsoft.AspNetCore.Components;
using Web.Modelos;

namespace Web.Componentes.Paginas.Catalogo;

public partial class DetalleReferencia : IDisposable
{
    private string? errorCarga;

    private bool cargando = true;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CuentaServicio Cuenta { get; set; } = default!;


    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public string Tipo { get; set; } = "categorias";

    [Parameter]
    public int Id { get; set; }

    private Referencia? Referencia;
    private string Titulo
    {
        get
        {
            if (Tipo == "autores")
                return "Autores";
            if (Tipo == "editoriales")
                return "Editoriales";
            return "Categorías";
        }
    }
    private string Singular
    {
        get
        {
            if (Tipo == "autores")
                return "autor";
            if (Tipo == "editoriales")
                return "editorial";
            return "categoría";
        }
    }
    private bool seguido, procesando;
    private string? correoCargado;

    protected override void OnInitialized() => Sesion.Cambiado += ActualizarVista;
    protected override Task OnParametersSetAsync() => CargarReferencia();

    private async Task CargarReferencia()
    {
        correoCargado = Sesion.Usuario?.Correo;
        cargando = true;
        errorCarga = null;
        try
        {
            var dto = await Api.Get<DetalleReferenciaDTO>($"{Tipo}/{Id}");
            var referencia = Presentacion.Referencia(dto.Referencia, Tipo);
            referencia.Descripcion = dto.Introduccion;
            referencia.Activo = dto.Activo;
            Referencia = referencia;
            seguido = dto.Seguido;
        }
        catch (InvalidOperationException e)
        {
            errorCarga = e.Message;
        }
        cargando = false;
    }
    private async void ActualizarVista()
    {
        await InvokeAsync(async () =>
        {
            if (correoCargado != Sesion.Usuario?.Correo)
                await CargarReferencia();
            StateHasChanged();
        });
    }
    public void Dispose() => Sesion.Cambiado -= ActualizarVista;
    private async Task AlternarSeguimiento()
    {
        if (!Sesion.RequerirAcceso())
            return;
        procesando = true;
        try
        {
            await Cuenta.AlternarSeguimiento(Tipo, Id, seguido);
            seguido = !seguido;
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

}
