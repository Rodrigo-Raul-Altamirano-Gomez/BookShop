using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Routing;

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

    private bool cuentaAbierta, accesoAbierto;
    private string? aviso, informacion;
    private static readonly (string Etiqueta, string Icono, string Ruta)[] EnlacesCuenta =
    [
        ("Mi perfil", "user", "/mi-perfil"),
        ("Deseados y favoritos", "heart", "/deseados"),
        ("Mis reseñas", "star", "/mis-resenas"),
        ("Mis pedidos", "cart", "/mis-pedidos"),
        ("Mis solicitudes", "follow", "/mis-solicitudes"),
        ("Mis seguimientos", "follow", "/mis-seguimientos"),
        ("Datos de compra", "card", "/datos-compra")
    ];
    protected override async Task OnInitializedAsync()
    {
        Sesion.Cambiado += Actualizar;
        Sesion.BusquedaCambiada += Actualizar;
        Sesion.AccesoSolicitado += AbrirAcceso;
        Sesion.AvisoSolicitado += MostrarAviso;
        Navegacion.LocationChanged += CambioUbicacion;
        await Catalogo.Inicializar();
    }

    private void Actualizar() => _ = InvokeAsync(StateHasChanged);
    private void CambioUbicacion(object? origen, LocationChangedEventArgs e)
    {
        Sesion.BuscarEnListado = null;
        Sesion.TextoBusqueda = "";
        Sesion.IndicacionBusqueda = "Buscar por título";
        cuentaAbierta = false;
        Actualizar();
    }

    private async Task Buscar()
    {
        if (Sesion.BuscarEnListado is not null)
            await Sesion.BuscarEnListado(Sesion.TextoBusqueda.Trim());
        else
            Navegacion.NavigateTo("/catalogo?q=" + Uri.EscapeDataString(Sesion.TextoBusqueda.Trim()));
    }
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
        Sesion.LibroComparacion = null;
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

    private void MostrarAviso(string mensaje)
    {
        aviso = mensaje;
        Actualizar();
    }

    private void CerrarAviso()
    {
        aviso = null;
    }

    public void Dispose()
    {
        Sesion.Cambiado -= Actualizar;
        Sesion.BusquedaCambiada -= Actualizar;
        Sesion.AccesoSolicitado -= AbrirAcceso;
        Sesion.AvisoSolicitado -= MostrarAviso;
        Navegacion.LocationChanged -= CambioUbicacion;
    }
}
