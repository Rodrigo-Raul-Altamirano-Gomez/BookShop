using Microsoft.AspNetCore.Components;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioAcceso
{
    [Inject] private CompraServicio Compras { get; set; } = default!;
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public EventCallback AlCerrar { get; set; }

    private bool registro, mostrarContrasena, reactivacion, enviando;
    private string correo = "", contrasena = "", confirmacion = "", nombre = "", urlFoto = "";
    private string foto => string.IsNullOrWhiteSpace(urlFoto) ? "/images/user-placeholder.svg" : urlFoto;
    private DateTime nacimiento = new(1998, 6, 12);
    private string? error;
    private void PestanaAcceso()
    {
        registro = false;
        error = null;
    }

    private void PestanaRegistro()
    {
        registro = true;
        error = null;
    }

    private void AlternarContrasena() => mostrarContrasena = !mostrarContrasena;
    private void QuitarFoto() => urlFoto = "";

    private async Task Enviar()
    {
        if (enviando)
            return;
        if (registro && contrasena != confirmacion)
        {
            error = "Las contraseñas no coinciden.";
            return;
        }
        enviando = true;
        try
        {
            string ruta;
            object datos;
            if (registro)
            {
                ruta = "cuenta/registro";
                datos = new RegistroUsuarioDTO
                {
                    Nombre = nombre.Trim(),
                    Email = correo.Trim(),
                    FechaNacimiento = DateOnly.FromDateTime(nacimiento),
                    Contrasenia = contrasena,
                    ConfirmacionContrasenia = confirmacion,
                    Imagen = Presentacion.ImagenDesdeUrl(urlFoto)
                };
            }
            else
            {
                ruta = "cuenta/sesion";
                if (reactivacion)
                    ruta = "cuenta/reactivacion";
                datos = new CredencialesDTO
                {
                    Email = correo.Trim(),
                    Contrasenia = contrasena
                };
            }
            var acceso = await Api.Post<AccesoUsuarioDTO>(ruta, datos);
            if (acceso.RequiereReactivacion)
            {
                reactivacion = true;
                return;
            }
            Sesion.MostrarPerfil(await Api.Get<PerfilUsuarioDTO>("cuenta"), acceso.Rol == "ADMIN");
            Compras.MostrarCarrito(await Api.Get<ResumenCompraDTO>("carrito"));
            Sesion.Notificar();
            await Cerrar();
        }
        catch (InvalidOperationException e)
        {
            error = e.Message;
        }
        finally
        {
            enviando = false;
        }
    }

    private Task Cerrar() => AlCerrar.InvokeAsync();
}
