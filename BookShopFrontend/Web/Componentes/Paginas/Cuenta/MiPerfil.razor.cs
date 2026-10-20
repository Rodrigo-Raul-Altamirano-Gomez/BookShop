using Microsoft.AspNetCore.Components.Forms;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MiPerfil
{
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
        if (Estado.Usuario is null)
            return;
        Nombre = Estado.Usuario.Nombre;
        Nacimiento = Estado.Usuario.Nacimiento;
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

    private void GuardarPerfil()
    {
        if (Estado.Usuario is null || string.IsNullOrWhiteSpace(Nombre))
            return;
        Estado.Usuario.Nombre = Nombre.Trim();
        Estado.Usuario.Nacimiento = Nacimiento;
        Cerrar();
        Estado.Notificar();
        Estado.Avisar("Datos personales actualizados.");
    }

    private void GuardarContrasena()
    {
        if (!ContrasenasCompletas)
            return;
        if (NuevaContrasena != ConfirmacionContrasena)
        {
            MensajeError = "Las contraseñas nuevas no coinciden.";
            return;
        }

        Cerrar();
        Estado.Avisar("Contraseña actualizada.");
    }

    private void Desactivar()
    {
        if (ContrasenaActual.Length == 0)
            return;
        Cerrar();
        Estado.Usuario = null;
        Estado.Notificar();
        Estado.Avisar("Tu cuenta ha sido desactivada.");
        Navegacion.NavigateTo("/");
    }

    private async Task CargarFoto(InputFileChangeEventArgs argumentos)
    {
        if (Estado.Usuario is null || CargandoImagen)
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
            Estado.Usuario.Imagen = $"data:{archivo.ContentType};base64,{Convert.ToBase64String(memoria
                .ToArray())}";
            Estado.Notificar();
            Estado.Avisar("Foto de perfil actualizada.");
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

    private void QuitarFoto()
    {
        if (Estado.Usuario is null || CargandoImagen)
            return;
        Estado.Usuario.Imagen = "/images/user-placeholder.svg";
        ErrorImagen = "";
        Estado.Notificar();
        Estado.Avisar("Foto de perfil retirada.");
    }
}

