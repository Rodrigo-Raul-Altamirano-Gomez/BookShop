using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioAcceso
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = default!;

    [Parameter]
    public EventCallback AlCerrar
    {
        get; set;
    }

    private bool registro, mostrarContrasena, reactivacion;
    private string correo = "", contrasena = "", confirmacion = "", nombre = "", foto =
         "/images/user-placeholder.svg";
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
    private void QuitarFoto() => foto = "/images/user-placeholder.svg";
    private async Task ElegirFoto(InputFileChangeEventArgs e)
    {
        var archivo = e.File;
        if (archivo.Size > 5 * 1024 * 1024 || archivo.ContentType is not ("image/jpeg" or "image/png"))
        {
            error = "Elige una imagen JPEG o PNG de hasta 5 MiB.";
            return;
        }

        try
        {
            await using var flujo = archivo.OpenReadStream(5 * 1024 * 1024);
            using var memoria = new MemoryStream();
            await flujo.CopyToAsync(memoria);
            foto = $"data:{archivo.ContentType};base64,{Convert.ToBase64String(memoria.ToArray())}";
            error = null;
        }
        catch (IOException)
        {
            error = "No se pudo cargar la imagen. Inténtalo otra vez.";
        }
    }

    private async Task Enviar()
    {
        // Este correo selecciona el ejemplo de error; no se verifican credenciales.
        if (!registro && correo.Equals("error@correo.pe", StringComparison.OrdinalIgnoreCase))
        {
            error = "Correo o contraseña incorrectos.";
            correo = contrasena = "";
            return;
        }

        if (registro && contrasena != confirmacion)
        {
            error = "Las contraseñas no coinciden.";
            return;
        }

        if (!reactivacion && !registro && correo.Equals("inactiva@correo.pe", StringComparison
            .OrdinalIgnoreCase))
        {
            reactivacion = true;
            return;
        }

        Estado.Usuario = new Perfil
        {

            Nombre = registro ? nombre.Trim() : "María López",

            Correo = correo.Trim(),

            Nacimiento = DateOnly.FromDateTime(nacimiento),

            Imagen = foto,

            EsAdministrador = correo.Trim().EndsWith("@admin.bookshop", StringComparison.OrdinalIgnoreCase)

        };
        Estado.Notificar();
        Estado.Avisar(reactivacion ? "Cuenta reactivada." : registro ? "Tu cuenta está lista."
            : "Bienvenida a BookShop.");
        await Cerrar();
    }

    private Task Cerrar() => AlCerrar.InvokeAsync();
}

