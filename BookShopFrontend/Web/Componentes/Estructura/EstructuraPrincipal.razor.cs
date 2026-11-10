using Microsoft.AspNetCore.Components;
using Web.Servicios;

namespace Web.Componentes.Estructura;

public partial class EstructuraPrincipal : IDisposable
{
    [Inject] protected CompraServicio Compras { get; set; } = default!;

    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Inject]
    private NavigationManager Navegacion { get; set; } = default!;

    private string textoBusqueda = "";
    private bool cuentaAbierta, accesoAbierto;
    private string? aviso, informacion;
    private CancellationTokenSource? temporizadorAviso;
    private static readonly (string Etiqueta, string Icono, string Ruta)[] EnlacesCuenta = [("Mi perfil",
         "user", "/mi-perfil"), ("Deseados y favoritos", "heart",
              "/deseados"), ("Mis seguimientos", "follow",
                   "/mis-seguimientos"), ("Datos de compra", "card", "/datos-compra")];
    protected override async Task OnInitializedAsync()
    {
        Sesion.Cambiado += Actualizar;
        Sesion.AccesoSolicitado += AbrirAcceso;
        Sesion.AvisoSolicitado += MostrarAviso;
        Navegacion.LocationChanged += CambioUbicacion;
        await Catalogo.Inicializar();
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
        if (Sesion.Usuario is null)
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

    private async Task CerrarSesion()
    {
        try
        {
            await Api.Delete("cuenta/sesion");
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        Sesion.LimpiarCuenta();
        cuentaAbierta = false;
        Navegacion.NavigateTo("/");
    }

    private void QuitarComparacion()
    {
        Sesion.IdComparacion = null;
        Sesion.Notificar();
    }

    private void CerrarInformacion() => informacion = null;
    private string TextoInformacion => informacion switch
    {
        "Ayuda" => ("Encuentra tu próxima lectura desde el catálogo. Para consultar "
            + "pedidos y datos de compra, abre Cuenta en el encabezado."),
        "Privacidad" => ("Los datos de tu cuenta se guardan en el backend. La sesión se "
            + "cierra al salir o cuando vence por inactividad."),
        _ => ("BookShop es un proyecto académico de una tienda de libros. Las "
            + "compras utilizan un pago simulado para fines académicos.")
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
        Sesion.Cambiado -= Actualizar;
        Sesion.AccesoSolicitado -= AbrirAcceso;
        Sesion.AvisoSolicitado -= MostrarAviso;
        Navegacion.LocationChanged -= CambioUbicacion;
        temporizadorAviso?.Cancel();
        temporizadorAviso?.Dispose();
    }
}
