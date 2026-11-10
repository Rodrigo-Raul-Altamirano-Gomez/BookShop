using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MiPerfil
{
    [Inject] protected HttpClientUtils Api { get; set; } = default!;

    private string ModalAbierto { get; set; } = "";
    private string Nombre { get; set; } = "";
    private DateOnly Nacimiento { get; set; }
    private string ContrasenaActual { get; set; } = "";
    private string NuevaContrasena { get; set; } = "";
    private string ConfirmacionContrasena { get; set; } = "";
    private bool MostrarContrasenas { get; set; }
    private string TipoContrasena => MostrarContrasenas ? "text" : "password";
    private bool ContrasenasCompletas => ContrasenaActual.Length > 0 && NuevaContrasena.Length > 0
        && ConfirmacionContrasena.Length > 0;
    private string MensajeError { get; set; } = "";
    private string ErrorImagen { get; set; } = "";
    private bool CargandoImagen { get; set; }

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
    }

    private async Task GuardarContrasena()
    {
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
    }

    private async Task Desactivar()
    {
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
    }

    private string urlFoto = "";
    private async Task GuardarFoto()
    {
        if (Sesion.Usuario is null || CargandoImagen)
            return;
        CargandoImagen = true;
        ErrorImagen = "";
        try
        {
            string ruta = "cuenta/imagen";
            if (!string.IsNullOrWhiteSpace(urlFoto))
                ruta += "?url=" + Uri.EscapeDataString(urlFoto.Trim());
            Sesion.MostrarPerfil(await Api.Put<PerfilUsuarioDTO>(ruta));
            Sesion.Notificar();
            Sesion.Avisar("Foto de perfil actualizada.");
        }
        catch (InvalidOperationException e)
        {
            ErrorImagen = e.Message;
        }
        finally
        {
            CargandoImagen = false;
        }
    }

    private async Task QuitarFoto()
    {
        try
        {
            Sesion.MostrarPerfil(await Api.Put<PerfilUsuarioDTO>("cuenta/imagen"));
            Sesion.Notificar();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
    }
}
