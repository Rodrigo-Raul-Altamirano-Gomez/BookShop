package me.DAO.usuario;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import me.DAO.CrudDAO;
import me.Model.dto.ImagenDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.ResumenActividadUsuarioDTO;
import me.Model.entities.usuario.Usuario;

public interface UsuarioDAO extends CrudDAO<Usuario> {
    Usuario leerParaActualizar(int id) throws SQLException;
    Usuario obtenerPorEmail(String email) throws SQLException;
    PerfilUsuarioDTO obtenerPerfil(int id) throws SQLException;
    boolean cambiarImagen(int id, ImagenDTO imagen, String usuario) throws SQLException;
    boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException;
    boolean cambiarContrasenia(int id, String hash, String usuario) throws SQLException;
    void actualizarTiempoTotal(int id, Duration tiempo, String usuario) throws SQLException;
    boolean incrementarPedidosEntregados(int id, String usuario) throws SQLException;
    List<ResumenActividadUsuarioDTO> listarActividadUsuarios(LocalDate inicio, LocalDate fin, Integer maximo)
            throws SQLException;
}
