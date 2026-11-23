package pe.edu.pucp.bookshop.bl.usuario;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.usuario.CambioContrasena;
import pe.edu.pucp.bookshop.model.usuario.Usuario;

public interface UsuarioBL {
    Usuario registrar(Usuario usuario) throws BusinessLogicException;
    Usuario login(String correo, String contrasena) throws BusinessLogicException;
    Usuario reactivar(String correo, String contrasena) throws BusinessLogicException;
    Usuario obtener(int id) throws BusinessLogicException;
    Usuario modificar(Usuario usuario) throws BusinessLogicException;
    void cambiarContrasena(int id, CambioContrasena cambio) throws BusinessLogicException;
    void desactivar(int id) throws BusinessLogicException;
}
