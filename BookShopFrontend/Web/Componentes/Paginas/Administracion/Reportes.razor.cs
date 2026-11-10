using Microsoft.AspNetCore.Components;
using Microsoft.JSInterop;
using Web.Servicios;

namespace Web.Componentes.Paginas.Administracion;

// RF18 (ventas por libro) y RF19 (usuarios por gasto mensual): formularios que descargan el PDF.
public partial class Reportes : IDisposable
{
    [Inject] private SesionUsuario Sesion { get; set; } = default!;
    [Inject] private DocumentoServicio Documentos { get; set; } = default!;

    private DateTime? inicio, fin, mes;
    private int? cantidadLibros, cantidadUsuarios;
    private bool generandoLibros, generandoUsuarios;
    private string? errorLibros, errorUsuarios;

    protected override void OnInitialized()
    {
        Sesion.Cambiado += Actualizar;
        var hoy = DateTime.Today;
        var inicioMes = new DateTime(hoy.Year, hoy.Month, 1);
        inicio = inicioMes;
        fin = hoy;
        mes = inicioMes.AddMonths(-1);
    }

    private async Task GenerarVentasLibros()
    {
        errorLibros = Validar(cantidadLibros);
        if (inicio is null || fin is null)
            errorLibros = "Selecciona el inicio y fin del período.";
        else if (fin < inicio)
            errorLibros = "La fecha final no puede ser anterior a la inicial.";
        if (errorLibros is not null)
            return;
        generandoLibros = true;
        errorLibros = await Ejecutar(() => Documentos.DescargarVentasLibros(
            DateOnly.FromDateTime(inicio!.Value), DateOnly.FromDateTime(fin!.Value), cantidadLibros));
        generandoLibros = false;
    }

    private async Task GenerarUsuarios()
    {
        errorUsuarios = mes is null ? "Selecciona un mes." : Validar(cantidadUsuarios);
        if (errorUsuarios is not null)
            return;
        generandoUsuarios = true;
        errorUsuarios = await Ejecutar(() => Documentos.DescargarUsuarios(mes!.Value.Year, mes.Value.Month, cantidadUsuarios));
        generandoUsuarios = false;
    }

    private static string? Validar(int? cantidad) =>
        cantidad is < 1 ? "La cantidad máxima debe ser positiva o quedar vacía." : null;

    // Devuelve el mensaje de error, o null si la descarga terminó bien.
    private static async Task<string?> Ejecutar(Func<Task> descarga)
    {
        try
        {
            await descarga();
            return null;
        }
        catch (InvalidOperationException e)
        {
            return e.Message;
        }
        catch (JSException)
        {
            return "El navegador no pudo guardar el archivo.";
        }
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Sesion.Cambiado -= Actualizar;
}
