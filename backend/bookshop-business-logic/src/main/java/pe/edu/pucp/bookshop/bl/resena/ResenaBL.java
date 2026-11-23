package pe.edu.pucp.bookshop.bl.resena;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.resena.Resena;
import pe.edu.pucp.bookshop.model.resena.ResenasLibro;

import java.util.List;

public interface ResenaBL {
    // idUsuario: quien consulta (0: visitante); incluirOcultas: vista del administrador
    ResenasLibro listarPorLibro(int idLibro, int idUsuario, boolean incluirOcultas) throws BusinessLogicException;
    List<Resena> listarPorUsuario(int idUsuario) throws BusinessLogicException;
    Resena insertar(Resena resena) throws BusinessLogicException;
    void modificar(Resena resena) throws BusinessLogicException;
    void eliminar(int id) throws BusinessLogicException;
    void alternarVoto(int idResena, int idUsuario) throws BusinessLogicException;
    void cambiarVisibilidad(int id, boolean activa) throws BusinessLogicException;
}
