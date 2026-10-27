package me.DAO.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DescuentoDTO;
import me.Model.dto.catalogo.EstadoDescuento;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;

public class DescuentoDAOImpl implements DescuentoDAO {
    @Override
    public void insertar(Descuento descuento) throws SQLException {
        validar(descuento);
        Connection conexion = TransactionContext.getConnection();
        if (haySuperposicion(descuento.getLibro().getId(), 0, descuento.getFechaInicio(), descuento.getFechaFin())) {
            throw new SQLException("El descuento se superpone con otro del libro.");
        }
        String sql = "INSERT INTO DESCUENTO (id_libro,porcentaje,fecha_inicio,fecha_fin,activo,"
                + " usuario_creacion,fecha_creacion) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, descuento.getLibro().getId());
            sentencia.setBigDecimal(2, descuento.getPorcentaje());
            sentencia.setTimestamp(3, Timestamp.valueOf(descuento.getFechaInicio()));
            sentencia.setTimestamp(4, Timestamp.valueOf(descuento.getFechaFin()));
            sentencia.setBoolean(5, descuento.isActivo());
            sentencia.setString(6, descuento.getUsuarioCreacion());
            sentencia.setTimestamp(7, Timestamp.valueOf(descuento.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id del descuento.");
                descuento.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Descuento descuento) throws SQLException {
        validar(descuento);
        Connection conexion = TransactionContext.getConnection();
        int idLibro;
        // La referencia persistida manda; editar datos nunca cambia el libro asociado.
        try (PreparedStatement consulta = conexion.prepareStatement("SELECT id_libro FROM DESCUENTO WHERE id=? AND eliminado=FALSE")) {
            consulta.setInt(1, descuento.getId());
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) throw new SQLException("El descuento no esta disponible.");
                idLibro = resultado.getInt("id_libro");
            }
        }
        if (haySuperposicion(idLibro, descuento.getId(), descuento.getFechaInicio(), descuento.getFechaFin())) {
            throw new SQLException("El descuento se superpone con otro del libro.");
        }
        String sql = "UPDATE DESCUENTO SET porcentaje=?,fecha_inicio=?,fecha_fin=?,activo=?,"
                + " usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, descuento.getPorcentaje());
            sentencia.setTimestamp(2, Timestamp.valueOf(descuento.getFechaInicio()));
            sentencia.setTimestamp(3, Timestamp.valueOf(descuento.getFechaFin()));
            sentencia.setBoolean(4, descuento.isActivo());
            sentencia.setString(5, descuento.getUsuarioActualizacion());
            sentencia.setInt(6, descuento.getId());
            sentencia.executeUpdate();
        }
    }

    @Override
    public Descuento leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM DESCUENTO WHERE id=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    @Override
    public Descuento leerParaActualizar(int id) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM DESCUENTO WHERE id=? FOR UPDATE")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE DESCUENTO SET eliminado=TRUE,activo=FALSE,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<DescuentoDTO> listarPorLibro(int idLibro, LocalDateTime fecha, int pagina, int tamanioPagina)
            throws SQLException {
        if (idLibro < 1 || fecha == null || pagina < 1 || tamanioPagina < 1) {
            throw new IllegalArgumentException("Libro, fecha y paginacion validos son obligatorios.");
        }
        List<DescuentoDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM DESCUENTO WHERE id_libro=? AND eliminado=FALSE")) {
                contar.setInt(1, idLibro);
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT id,porcentaje,fecha_inicio,fecha_fin FROM DESCUENTO"
                    + " WHERE id_libro=? AND eliminado=FALSE ORDER BY fecha_inicio DESC,id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idLibro);
                sentencia.setInt(2, tamanioPagina);
                sentencia.setLong(3, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        LocalDateTime inicio = resultado.getTimestamp("fecha_inicio").toLocalDateTime();
                        LocalDateTime fin = resultado.getTimestamp("fecha_fin").toLocalDateTime();
                        EstadoDescuento estado = fecha.isBefore(inicio) ? EstadoDescuento.PROXIMO
                                : fecha.isAfter(fin) ? EstadoDescuento.VENCIDO : EstadoDescuento.VIGENTE;
                        items.add(new DescuentoDTO(resultado.getInt("id"), resultado.getBigDecimal("porcentaje"), inicio, fin, estado));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, pagina, tamanioPagina);
    }

    @Override
    public Descuento obtenerVigente(int idLibro, LocalDateTime fecha) throws SQLException {
        if (idLibro < 1 || fecha == null) throw new IllegalArgumentException("Libro y fecha validos son obligatorios.");
        String sql = "SELECT * FROM DESCUENTO WHERE id_libro=? AND eliminado=FALSE"
                + " AND fecha_inicio<=? AND fecha_fin>=? ORDER BY fecha_inicio DESC,id DESC LIMIT 1";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idLibro);
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(3, Timestamp.valueOf(fecha));
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                Descuento descuento = mapear(resultado);
                descuento.setActivo(true);
                return descuento;
            }
        }
    }

    @Override
    public void actualizarVigencias(LocalDateTime fecha, String usuario) throws SQLException {
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        String sql = "UPDATE DESCUENTO SET activo=(fecha_inicio<=? AND fecha_fin>=?),usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE eliminado=FALSE AND activo<>(fecha_inicio<=? AND fecha_fin>=?)";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            sentencia.setString(3, usuario);
            sentencia.setTimestamp(4, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(5, Timestamp.valueOf(fecha));
            sentencia.executeUpdate();
        }
    }

    @Override
    public boolean haySuperposicion(int idLibro, int idExcluir, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        if (idLibro < 1 || idExcluir < 0 || inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Libro e intervalo validos son obligatorios.");
        }
        Connection conexion = TransactionContext.getConnection();
        bloquearLibro(conexion, idLibro);
        // El bloqueo tambien consulta datos actuales si la transaccion ya tenia una lectura previa.
        String sql = "SELECT 1 FROM DESCUENTO WHERE id_libro=? AND id<>? AND eliminado=FALSE"
                + " AND fecha_inicio<=? AND fecha_fin>=? LIMIT 1 FOR UPDATE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idLibro);
            sentencia.setInt(2, idExcluir);
            sentencia.setTimestamp(3, Timestamp.valueOf(fin));
            sentencia.setTimestamp(4, Timestamp.valueOf(inicio));
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private void bloquearLibro(Connection conexion, int idLibro) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT id FROM LIBRO WHERE id=? AND eliminado=FALSE FOR UPDATE")) {
            sentencia.setInt(1, idLibro);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new SQLException("El libro no esta disponible.");
            }
        }
    }

    private void validar(Descuento descuento) {
        if (descuento == null || descuento.getLibro() == null || descuento.getLibro().getId() < 1
                || descuento.getPorcentaje() == null || descuento.getPorcentaje().signum() <= 0
                || descuento.getPorcentaje().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || descuento.getFechaInicio() == null || descuento.getFechaFin() == null
                || !descuento.getFechaFin().isAfter(descuento.getFechaInicio())) {
            throw new IllegalArgumentException("Descuento y vigencia validos son obligatorios.");
        }
    }

    private Descuento mapear(ResultSet resultado) throws SQLException {
        Descuento descuento = new Descuento();
        descuento.setId(resultado.getInt("id"));
        Libro libro = new Libro();
        libro.setId(resultado.getInt("id_libro"));
        descuento.setLibro(libro);
        descuento.setPorcentaje(resultado.getBigDecimal("porcentaje"));
        descuento.setFechaInicio(resultado.getTimestamp("fecha_inicio").toLocalDateTime());
        descuento.setFechaFin(resultado.getTimestamp("fecha_fin").toLocalDateTime());
        descuento.setActivo(resultado.getBoolean("activo"));
        descuento.setEliminado(resultado.getBoolean("eliminado"));
        descuento.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        descuento.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
        descuento.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
        descuento.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
        return descuento;
    }
}
