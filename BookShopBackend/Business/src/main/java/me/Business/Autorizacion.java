package me.Business;

import java.sql.SQLException;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.DAO.usuario.UsuarioDAO;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;

// El id viene de la sesión autenticada del servidor, nunca de un selector del cliente.
public class Autorizacion {
    private final UsuarioDAO usuarios;
    public Autorizacion(UsuarioDAO usuarios) { this.usuarios = usuarios; }

    public Usuario usuarioActivo(int idActor) throws NegocioException {
        if (idActor <= 0) throw new AccesoDenegadoException("Debes iniciar sesión.");
        try {
            Usuario actor = usuarios.leer(idActor);
            if (actor == null || actor.isEliminado())
                throw new AccesoDenegadoException("La sesión no corresponde a una cuenta disponible.");
            if (!actor.isActivo()) throw new CuentaInactivaException("La cuenta está desactivada.");
            return actor;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo comprobar la sesión.", e);
        }
    }

    public Usuario administrador(int idActor) throws NegocioException {
        Usuario actor = usuarioActivo(idActor);
        if (actor.getRol() != Rol.ADMIN)
            throw new AccesoDenegadoException("Esta operación requiere una cuenta administradora.");
        return actor;
    }

    public Usuario propietario(int idActor, int idPropietario) throws NegocioException {
        Usuario actor = usuarioActivo(idActor);
        if (actor.getId() != idPropietario)
            throw new AccesoDenegadoException("Este recurso pertenece a otra cuenta.");
        return actor;
    }
}
