using Microsoft.AspNetCore.Components.Forms;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MiPerfil
{
    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    private string ModalAbierto { get; set; } = "";
    private string Nombre { get; set; } = "";
    private DateOnly Nacimiento
    {
        get; set;
    }
    private string ContrasenaActual { get; set; } = "";
    private string NuevaContrasena { get; set; } = "";
    private string ConfirmacionContrasena { get; set; } = "";
    private bool MostrarContrasenas
    {
        get; set;
    }
    private string TipoContrasena => MostrarContrasenas ? "text" : "password";
    private bool ContrasenasCompletas => ContrasenaActual.Length > 0 && NuevaContrasena.Length > 0
        && ConfirmacionContrasena.Length > 0;
    private string MensajeError { get; set; } = "";
    private string ErrorImagen { get; set; } = "";
    private bool CargandoImagen
    {
        get; set;
    }

    private void EditarPerfil()
    {
        if (Sesion.Usuario is null)
            return;
        Nombre = Sesion.Usuario.Nombre;
        Nacimiento = Sesion.Usuario.Nacimiento;
        MensajeError = "";
        ModalAbierto = "profile";
    }

    private void AbrirContrasena()
    {
        Cerrar();
        ModalAbierto = "password";
    }

    private void AbrirDesactivacion()
    {
        Cerrar();
        ModalAbierto = "deactivate";
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
        catch (InvalidOperationException e) { MensajeError = e.Message; }
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
        catch (InvalidOperationException e) { MensajeError = e.Message; }
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
        catch (InvalidOperationException e) { MensajeError = e.Message; }
    }

    private async Task CargarFoto(InputFileChangeEventArgs argumentos)
    {
        if (Sesion.Usuario is null || CargandoImagen)
            return;
        var archivo = argumentos.File;
        ErrorImagen = "";
        if (archivo.ContentType is not ("image/jpeg" or "image/png") || archivo.Size > 5 * 1024 * 1024)
        {
            ErrorImagen = "Selecciona una imagen JPEG o PNG de hasta 5 MiB.";
            return;
        }

        CargandoImagen = true;
        try
        {
            using var flujo = archivo.OpenReadStream(5 * 1024 * 1024);
            using var memoria = new MemoryStream();
            await flujo.CopyToAsync(memoria);
            Sesion.MostrarPerfil(await Api.Put<PerfilUsuarioDTO>("cuenta/imagen", new ImagenDTO
            {
                Contenido = memoria.ToArray().Select(n => (int)unchecked((sbyte)n)).ToArray(),
                TipoMime = archivo.ContentType
            }));
            Sesion.Notificar();
            Sesion.Avisar("Foto de perfil actualizada.");
        }
        catch (InvalidOperationException e)
        {
            ErrorImagen = e.Message;
        }
        catch (IOException)
        {
            ErrorImagen = "No se pudo cargar la imagen. Inténtalo de nuevo.";
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

