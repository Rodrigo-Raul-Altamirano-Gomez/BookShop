using Microsoft.AspNetCore.Components;
using Web.Demostracion;

namespace Web.Componentes.Estructura;

public partial class EstructuraPrincipal : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    private string textoBusqueda = "";
    private bool cuentaAbierta, accesoAbierto;
    private string? aviso, informacion;
    private CancellationTokenSource? temporizadorAviso;
    private static readonly (string Etiqueta, string Icono, string Ruta)[] EnlacesCuenta = [("Mi perfil",
         "user", "/mi-perfil"), ("Mis pedidos", "book", "/mis-pedidos"), ("Deseados y favoritos", "heart",
              "/deseados"), ("Mis reseñas", "star", "/mis-resenas"), ("Mis seguimientos", "follow",
                   "/mis-seguimientos"), ("Datos de compra", "card", "/datos-compra"), ("Mis solicitudes",
                        "flag", "/mis-solicitudes")];
    protected override void OnInitialized()
    {
        Estado.Cambiado += Actualizar;
        Estado.AccesoSolicitado += AbrirAcceso;
        Estado.AvisoSolicitado += MostrarAviso;
        Navegacion.LocationChanged += CambioUbicacion;
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void CambioUbicacion(object? origen, Microsoft.AspNetCore.Components.Routing
        .LocationChangedEventArgs e)
    {
        cuentaAbierta = false;
        Actualizar();
    }

    private void Buscar() => Navegacion.NavigateTo("/catalogo?q=" + Uri.EscapeDataString(textoBusqueda));
    private void AbrirCuenta()
    {
        if (Estado.Usuario is null)
            AbrirAcceso();
        else
            cuentaAbierta = true;
    }

    private void AbrirAcceso()
    {
        accesoAbierto = true;
        cuentaAbierta = false;
        Actualizar();
    }

    private void CerrarAcceso() => accesoAbierto = false;
    private void CerrarCuenta() => cuentaAbierta = false;
    private void NavegarCuenta(string ruta)
    {
        cuentaAbierta = false;
        Navegacion.NavigateTo(ruta);
    }

    private void CerrarSesion()
    {
        Estado.Usuario = null;
        cuentaAbierta = false;
        Estado.Notificar();
        Navegacion.NavigateTo("/");
        Estado.Avisar("Has cerrado sesión.");
    }

    private void QuitarComparacion()
    {
        Estado.IdComparacion = null;
        Estado.Notificar();
    }

    private void CerrarInformacion() => informacion = null;
    private string TextoInformacion => informacion switch
    {
        "Ayuda" => ("Encuentra tu próxima lectura desde el catálogo. Para consultar "
            + "pedidos y datos de compra, abre Cuenta en el encabezado."),
        "Privacidad" => ("Esta versión utiliza datos de ejemplo. Los cambios de pantalla se "
            + "mantienen durante la sesión y se restablecen al recargar."),
        _ => ("BookShop es un proyecto académico de una tienda de libros. Las "
            + "compras y solicitudes de esta versión son demostraciones visuales.")
    };

    private async void MostrarAviso(string mensaje)
    {
        temporizadorAviso?.Cancel();
        temporizadorAviso?.Dispose();
        temporizadorAviso = new();
        var token = temporizadorAviso.Token;
        await InvokeAsync(() =>
        {
            aviso = mensaje;
            StateHasChanged();
        });
        try
        {
            await Task.Delay(5000, token);
            await InvokeAsync(() =>
            {
                aviso = null;
                StateHasChanged();
            });
        }
        catch (OperationCanceledException)
        {
        }
    }

    private void CerrarAviso()
    {
        temporizadorAviso?.Cancel();
        aviso = null;
    }

    public void Dispose()
    {
        Estado.Cambiado -= Actualizar;
        Estado.AccesoSolicitado -= AbrirAcceso;
        Estado.AvisoSolicitado -= MostrarAviso;
        Navegacion.LocationChanged -= CambioUbicacion;
        temporizadorAviso?.Cancel();
        temporizadorAviso?.Dispose();
    }
}

