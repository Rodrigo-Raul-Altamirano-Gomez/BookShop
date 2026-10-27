package me.Business.usuario;

import java.time.Duration;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ImagenDTO;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CambioContraseniaDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.DatosPerfilDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;

public interface UsuarioBO {
    AccesoUsuarioDTO registrar(RegistroUsuarioDTO datos) throws NegocioException;
    AccesoUsuarioDTO iniciarSesion(CredencialesDTO credenciales) throws NegocioException;
    AccesoUsuarioDTO reactivar(CredencialesDTO credenciales) throws NegocioException;
    PerfilUsuarioDTO obtenerPerfil(int idActor) throws NegocioException;
    PerfilUsuarioDTO modificarPerfil(int idActor, DatosPerfilDTO datos) throws NegocioException;
    PerfilUsuarioDTO cambiarImagen(int idActor, ImagenDTO imagen) throws NegocioException;
    void cambiarContrasenia(int idActor, CambioContraseniaDTO datos) throws NegocioException;
    void desactivar(int idActor, String contraseniaActual) throws NegocioException;
    void cerrarSesion(int idActor, Duration tiempoSesion) throws NegocioException;
}
