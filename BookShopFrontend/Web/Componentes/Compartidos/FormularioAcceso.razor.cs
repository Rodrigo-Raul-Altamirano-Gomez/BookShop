using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Forms;
using Web.Servicios;
using Web.Modelos;

namespace Web.Componentes.Compartidos;

public partial class FormularioAcceso
{
    [Inject] private CompraServicio Compras { get; set; } = default!;
    [Microsoft.AspNetCore.Components.Inject] protected HttpClientUtils Api { get; set; } = default!;

    [Inject]
    private SesionUsuario Sesion { get; set; } = default!;

    [Parameter]
    public EventCallback AlCerrar
    {
        get; set;
    }

    private bool registro, mostrarContrasena, reactivacion, enviando;
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
            var ruta = registro ? "cuenta/registro" : reactivacion ? "cuenta/reactivacion" : "cuenta/sesion";
            object datos = registro
                ? new RegistroUsuarioDTO
                {
                    Nombre = nombre.Trim(),
                    Email = correo.Trim(),
                    FechaNacimiento = DateOnly.FromDateTime(nacimiento),
                    Contrasenia = contrasena,
                    ConfirmacionContrasenia = confirmacion,
                    Imagen = Presentacion.LeerImagen(foto)
                }
                : new CredencialesDTO
                {
                    Email = correo.Trim(),
                    Contrasenia = contrasena
                };
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
        catch (InvalidOperationException e) { error = e.Message; }
        finally { enviando = false; }
    }

    private Task Cerrar() => AlCerrar.InvokeAsync();
}

