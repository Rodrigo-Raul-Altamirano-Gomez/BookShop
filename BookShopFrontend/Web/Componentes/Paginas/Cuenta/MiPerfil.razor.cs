using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MiPerfil
{
    private bool procesando;
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    private string ModalAbierto { get; set; } = "";
    private string Nombre { get; set; } = "";
    private DateOnly Nacimiento { get; set; }
    private string ContrasenaActual { get; set; } = "";
    private string NuevaContrasena { get; set; } = "";
    private string ConfirmacionContrasena { get; set; } = "";
    private bool MostrarContrasenas { get; set; }
    private string TipoContrasena => MostrarContrasenas ? "text" : "password";
    private string MensajeError { get; set; } = "";
    private string ErrorImagen { get; set; } = "";

    private void EditarPerfil()
    {
        if (Sesion.Usuario is null)
            return;
        Nombre = Sesion.Usuario.Nombre;
        Nacimiento = Sesion.Usuario.Nacimiento;
        MensajeError = "";
        ModalAbierto = "perfil";
    }

    private void AbrirContrasena()
    {
        Cerrar();
        ModalAbierto = "password";
    }

    private void AbrirDesactivacion()
    {
        Cerrar();
        ModalAbierto = "desactivar";
    }

    private void AlternarContrasenas() => MostrarContrasenas = !MostrarContrasenas;
    private void Cerrar()
    {
        ModalAbierto = "";
        MensajeError = "";
        ContrasenaActual = "";
        NuevaContrasena = "";
        ConfirmacionContrasena = "";
        MostrarContrasenas = false;
    }

    private async Task GuardarPerfil()
    {
        procesando = true;
        try
        {
            Sesion.MostrarPerfil(await Api.Put<PerfilUsuarioDTO>("cuenta", new DatosPerfilDTO
            {
                Nombre = Nombre.Trim(),
                FechaNacimiento = Nacimiento
            }));
            Cerrar();
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            MensajeError = e.Message;
        }
        procesando = false;
    }

    private async Task GuardarContrasena()
    {
        procesando = true;
        try
        {
            await Api.Put("cuenta/contrasenia", new CambioContraseniaDTO
            {
                ContraseniaActual = ContrasenaActual,
                ContraseniaNueva = NuevaContrasena,
                ConfirmacionContrasenia = ConfirmacionContrasena
            });
            Cerrar();
            Sesion.Avisar("Contraseña actualizada.");
        }
        catch (InvalidOperationException e)
        {
            MensajeError = e.Message;
        }
        procesando = false;
    }

    private async Task Desactivar()
    {
        procesando = true;
        try
        {
            await Api.Post("cuenta/desactivacion", new
            {
                contraseniaActual = ContrasenaActual
            });
            Sesion.LimpiarCuenta();
            Cerrar();
            Navegacion.NavigateTo("/");
        }
        catch (InvalidOperationException e)
        {
            MensajeError = e.Message;
        }
        procesando = false;
    }

    private string urlFoto = "";
    private async Task GuardarFoto(bool quitar)
    {
        if (Sesion.Usuario is null)
            return;
        procesando = true;
        ErrorImagen = "";
        try
        {
            string ruta = "cuenta/imagen";
            if (!quitar)
                ruta += "?url=" + Uri.EscapeDataString(urlFoto.Trim());
            Sesion.MostrarPerfil(await Api.Put<PerfilUsuarioDTO>(ruta));
            Sesion.Notificar();
            urlFoto = "";
            Sesion.Avisar("Foto de perfil actualizada.");
        }
        catch (InvalidOperationException e)
        {
            ErrorImagen = e.Message;
        }
        procesando = false;
    }
}
