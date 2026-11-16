package me.DAO.usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.usuario.TipoSeguimiento;

public class SeguimientoDAOImpl implements SeguimientoDAO {

    @Override
    public void guardar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws
        SQLException {
        String sql = "INSERT INTO SEGUIMIENTO (tipo_seguimiento, id_usuario, " + columnaReferencia(tipo) +
            ", usuario_creacion) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
            "usuario_actualizacion = CASE WHEN eliminado THEN ? ELSE usuario_actualizacion END, " +
            "fecha_actualizacion = CASE WHEN eliminado THEN CURRENT_TIMESTAMP ELSE fecha_actualizacion END, "
            + "eliminado = FALSE";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, tipo.name());
            sentencia.setInt(2, idUsuario);
            sentencia.setInt(3, idReferencia);
            sentencia.setString(4, usuario);
            sentencia.setString(5, usuario);
            sentencia.executeUpdate();
        }
    }

    @Override
    public boolean retirar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws
        SQLException {
        String sql = "UPDATE SEGUIMIENTO SET eliminado = TRUE, usuario_actualizacion = ?, " +
            "fecha_actualizacion = CURRENT_TIMESTAMP WHERE tipo_seguimiento = ? " + "AND id_usuario = ? AND "
            + columnaReferencia(tipo) + " = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
            PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setString(2, tipo.name());
            sentencia.setInt(3, idUsuario);
            sentencia.setInt(4, idReferencia);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listarPorUsuario(int idUsuario, TipoSeguimiento tipo,
        OrdenReferencia orden, int pagina, int tamanioPagina, String nombre) throws SQLException {
        String condiciones = " FROM SEGUIMIENTO s JOIN " + tipo.name() + " r ON r.id = s." +
            columnaReferencia(tipo) + " WHERE s.tipo_seguimiento = ? AND s.id_usuario = ? " +
            "AND NOT s.eliminado AND r.activo AND NOT r.eliminado AND LOCATE(LOWER(?),LOWER(r.nombre))>0";
        String ordenSQL = orden == OrdenReferencia.MAS_RECIENTES ?
            "COALESCE(s.fecha_actualizacion, s.fecha_creacion) DESC, s.id DESC" : "r.nombre, r.id";
        List<TarjetaReferenciaDTO> tarjetas = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*)" + condiciones)) {
                sentencia.setString(1, tipo.name());
                sentencia.setInt(2, idUsuario);
                sentencia.setString(3, nombre);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT r.id, r.nombre, r.imagen" + condiciones + " ORDER BY " + ordenSQL +
                " LIMIT ? OFFSET ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, tipo.name());
                sentencia.setInt(2, idUsuario);
                sentencia.setString(3, nombre);
                sentencia.setInt(4, tamanioPagina);
                sentencia.setLong(5, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        String imagen = resultado.getString("imagen");
                        tarjetas.add(new TarjetaReferenciaDTO(resultado.getInt("id"),
                            resultado.getString("nombre"), imagen));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(tarjetas, total, pagina, tamanioPagina);
    }

    private String columnaReferencia(TipoSeguimiento tipo) {
        return switch(tipo) {
            case AUTOR ->
                "id_autor";
            case CATEGORIA ->
                "id_categoria";
            case EDITORIAL ->
                "id_editorial";
        };
    }
}
