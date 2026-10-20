package me.DAO.solicitud;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.DAO.catalogo.ResenaDAOImpl;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.ResumenPedidoDTO;
import me.Model.dto.opinion.DatosResenaDTO;
import me.Model.dto.solicitud.*;
import me.Model.entities.compra.EstadoPedido;
import me.Model.entities.compra.Pedido;
import me.Model.entities.opinion.Resena;
import me.Model.entities.solicitud.*;
import me.Model.entities.usuario.Usuario;

public class SolicitudDAOImpl implements SolicitudDAO {
    @Override
    public void insertar(Solicitud solicitud) throws SQLException {
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE || solicitud.isEliminado())
            throw new IllegalArgumentException("Una nueva solicitud debe estar pendiente.");
        TipoSolicitud tipo = tipoDe(solicitud);
        Connection conexion = TransactionContext.getConnection();
        // Business valida plazos, intentos y permisos. Los bloqueos protegen la unicidad
        // de pendientes y la captura del contenido que realmente se reportó.
        if (solicitud instanceof SolicitudDevolucion devolucion) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT id FROM PEDIDO WHERE id = ? AND id_usuario = ? FOR UPDATE")) {
                statement.setInt(1, devolucion.getPedido().getId());
                statement.setInt(2, devolucion.getUsuario().getId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) throw new SQLException("El pedido de la devolucion no existe para este usuario.");
                }
            }
            try (PreparedStatement statement = conexion.prepareStatement("SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'DEVOLUCION' AND id_pedido = ? AND estado = 'PENDIENTE' AND NOT eliminado")) {
                statement.setInt(1, devolucion.getPedido().getId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) throw new SQLException("Ya existe una devolucion pendiente para el pedido.");
                }
            }
        } else if (solicitud instanceof SolicitudReporteResena reporte) {
            Resena original = new ResenaDAOImpl().leerParaActualizar(reporte.getResena().getId());
            if (original == null) throw new SQLException("La resena reportada no existe.");
            reporte.setPuntuacionOriginal(original.getPuntuacion());
            reporte.setComentarioOriginal(original.getComentario());
            try (PreparedStatement statement = conexion.prepareStatement("SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'REPORTE_RESENA' AND id_usuario = ? AND id_resena = ? AND estado = 'PENDIENTE' AND NOT eliminado")) {
                statement.setInt(1, reporte.getUsuario().getId());
                statement.setInt(2, reporte.getResena().getId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) throw new SQLException("Ya existe un reporte pendiente del usuario para la resena.");
                }
            }
        }
        String sql = """
                INSERT INTO SOLICITUD (tipo_solicitud, id_usuario, id_pedido, motivo_devolucion,
                    id_resena, motivo_reporte, puntuacion_original, comentario_original,
                    comentario, estado, fecha_resolucion, eliminado, usuario_creacion, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', NULL, FALSE, ?, ?)
                """;
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tipo.name());
            statement.setInt(2, solicitud.getUsuario().getId());
            if (solicitud instanceof SolicitudDevolucion devolucion) {
                statement.setInt(3, devolucion.getPedido().getId());
                statement.setString(4, devolucion.getMotivo().name());
                statement.setNull(5, Types.INTEGER);
                statement.setNull(6, Types.VARCHAR);
                statement.setNull(7, Types.TINYINT);
                statement.setNull(8, Types.LONGVARCHAR);
            } else {
                SolicitudReporteResena reporte = (SolicitudReporteResena) solicitud;
                statement.setNull(3, Types.INTEGER);
                statement.setNull(4, Types.VARCHAR);
                statement.setInt(5, reporte.getResena().getId());
                statement.setString(6, reporte.getMotivo().name());
                statement.setInt(7, reporte.getPuntuacionOriginal());
                statement.setString(8, reporte.getComentarioOriginal());
            }
            statement.setString(9, solicitud.getComentario());
            statement.setString(10, solicitud.getUsuarioCreacion());
            statement.setTimestamp(11, Timestamp.valueOf(solicitud.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No se genero el identificador de la solicitud.");
                solicitud.setId(rs.getInt(1));
            }
        }
    }

    private TipoSolicitud tipoDe(Solicitud solicitud) {
        if (solicitud instanceof SolicitudDevolucion) return TipoSolicitud.DEVOLUCION;
        if (solicitud instanceof SolicitudReporteResena) return TipoSolicitud.REPORTE_RESENA;
        throw new IllegalArgumentException("Tipo de solicitud no reconocido.");
    }

    @Override
    public void modificar(Solicitud solicitud) throws SQLException {
        TipoSolicitud tipo = tipoDe(solicitud);
        String columnaMotivo = tipo == TipoSolicitud.DEVOLUCION ? "motivo_devolucion" : "motivo_reporte";
        String motivo = solicitud instanceof SolicitudDevolucion devolucion ? devolucion.getMotivo().name()
                : ((SolicitudReporteResena) solicitud).getMotivo().name();
        String sql = "UPDATE SOLICITUD SET " + columnaMotivo + " = ?, comentario = ?, usuario_actualizacion = ?, fecha_actualizacion = ?"
                + " WHERE id = ? AND tipo_solicitud = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, motivo);
            statement.setString(2, solicitud.getComentario());
            statement.setString(3, solicitud.getUsuarioActualizacion());
            statement.setTimestamp(4, Timestamp.valueOf(solicitud.getFechaActualizacion() == null ? LocalDateTime.now() : solicitud.getFechaActualizacion()));
            statement.setInt(5, solicitud.getId());
            statement.setString(6, tipo.name());
            if (statement.executeUpdate() != 1) throw new SQLException("Solo puede editarse una solicitud pendiente.");
        }
    }

    @Override
    public Solicitud leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection()) { return leerRegistro(conexion, id, false); }
    }

    @Override
    public Solicitud leerParaActualizar(int id) throws SQLException {
        return leerRegistro(TransactionContext.getConnection(), id, true);
    }

    private Solicitud leerRegistro(Connection conexion, int id, boolean bloquear) throws SQLException {
        try (PreparedStatement statement = conexion.prepareStatement("SELECT * FROM SOLICITUD WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapearSolicitud(rs) : null; }
        }
    }

    private Solicitud mapearSolicitud(ResultSet rs) throws SQLException {
        Solicitud solicitud;
        if (TipoSolicitud.valueOf(rs.getString("tipo_solicitud")) == TipoSolicitud.DEVOLUCION) {
            SolicitudDevolucion devolucion = new SolicitudDevolucion();
            Pedido pedido = new Pedido();
            pedido.setId(rs.getInt("id_pedido"));
            devolucion.setPedido(pedido);
            devolucion.setMotivo(MotivoDevolucion.valueOf(rs.getString("motivo_devolucion")));
            solicitud = devolucion;
        } else {
            SolicitudReporteResena reporte = new SolicitudReporteResena();
            Resena resena = new Resena();
            resena.setId(rs.getInt("id_resena"));
            reporte.setResena(resena);
            reporte.setMotivo(MotivoReporteResena.valueOf(rs.getString("motivo_reporte")));
            reporte.setPuntuacionOriginal(rs.getInt("puntuacion_original"));
            reporte.setComentarioOriginal(rs.getString("comentario_original"));
            solicitud = reporte;
        }
        solicitud.setId(rs.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario"));
        solicitud.setUsuario(usuario);
        solicitud.setComentario(rs.getString("comentario"));
        solicitud.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
        solicitud.setFechaResolucion(rs.getObject("fecha_resolucion", LocalDateTime.class));
        solicitud.setEliminado(rs.getBoolean("eliminado"));
        solicitud.setUsuarioCreacion(rs.getString("usuario_creacion"));
        solicitud.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        solicitud.setUsuarioActualizacion(rs.getString("usuario_actualizacion"));
        solicitud.setFechaActualizacion(rs.getObject("fecha_actualizacion", LocalDateTime.class));
        return solicitud;
    }

    @Override
    public boolean eliminar(int id, String actor) throws SQLException {
        String sql = "UPDATE SOLICITUD SET eliminado = TRUE, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, actor);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listar(int idUsuario, FiltroSolicitudDTO filtro) throws SQLException {
        if (filtro == null) filtro = new FiltroSolicitudDTO();
        if (filtro.tipo() == null) throw new IllegalArgumentException("Debe indicarse el tipo de solicitud.");
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1) throw new IllegalArgumentException("Pagina y tamanio deben ser positivos.");
        String condiciones = " WHERE s.tipo_solicitud = ? AND NOT s.eliminado";
        if (idUsuario > 0) condiciones += " AND s.id_usuario = ?";
        if (!filtro.estados().isEmpty()) condiciones += " AND s.estado IN (" + String.join(",", java.util.Collections.nCopies(filtro.estados().size(), "?")) + ")";
        String sql = "SELECT s.*, u.nombre AS nombre_solicitante FROM SOLICITUD s JOIN USUARIO u ON u.id = s.id_usuario"
                + condiciones + " ORDER BY s.fecha_creacion DESC, s.id DESC LIMIT ? OFFSET ?";
        List<ResumenSolicitudDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*) FROM SOLICITUD s" + condiciones)) {
                int parametro = 1;
                statement.setString(parametro++, filtro.tipo().name());
                if (idUsuario > 0) statement.setInt(parametro++, idUsuario);
                for (EstadoSolicitud estado : filtro.estados()) statement.setString(parametro++, estado.name());
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                int parametro = 1;
                statement.setString(parametro++, filtro.tipo().name());
                if (idUsuario > 0) statement.setInt(parametro++, idUsuario);
                for (EstadoSolicitud estado : filtro.estados()) statement.setString(parametro++, estado.name());
                statement.setInt(parametro++, filtro.tamanioPagina());
                statement.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet rs = statement.executeQuery()) { while (rs.next()) items.add(mapearResumen(rs)); }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    private ResumenSolicitudDTO mapearResumen(ResultSet rs) throws SQLException {
        TipoSolicitud tipo = TipoSolicitud.valueOf(rs.getString("tipo_solicitud"));
        String motivo;
        if (tipo == TipoSolicitud.DEVOLUCION) {
            motivo = switch (MotivoDevolucion.valueOf(rs.getString("motivo_devolucion"))) {
                case LIBRO_DANADO -> "Libro dañado";
                case LIBRO_INCORRECTO -> "Libro incorrecto";
                case PEDIDO_INCOMPLETO -> "Pedido incompleto";
                case NO_CUMPLE_EXPECTATIVAS -> "No cumple expectativas";
                case OTRO -> "Otro";
            };
        } else {
            motivo = switch (MotivoReporteResena.valueOf(rs.getString("motivo_reporte"))) {
                case CONTENIDO_OFENSIVO -> "Contenido ofensivo";
                case PUBLICIDAD_O_SPAM -> "Publicidad o spam";
                case CONTENIDO_AJENO_AL_LIBRO -> "Contenido ajeno al libro";
                case OTRO -> "Otro";
            };
        }
        return new ResumenSolicitudDTO(rs.getInt("id"), tipo, rs.getObject("fecha_creacion", LocalDateTime.class),
                EstadoSolicitud.valueOf(rs.getString("estado")), rs.getString("nombre_solicitante"), motivo,
                rs.getInt(tipo == TipoSolicitud.DEVOLUCION ? "id_pedido" : "id_resena"));
    }

    @Override
    public DetalleDevolucionDTO obtenerDetalleDevolucion(int id) throws SQLException {
        String sql = """
                SELECT s.*, u.nombre AS nombre_solicitante, p.fecha_pago, p.estado AS estado_pedido,
                    p.cantidad_total_libros, p.total
                FROM SOLICITUD s JOIN USUARIO u ON u.id = s.id_usuario JOIN PEDIDO p ON p.id = s.id_pedido
                WHERE s.id = ? AND s.tipo_solicitud = 'DEVOLUCION'
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                ResumenPedidoDTO pedido = new ResumenPedidoDTO(rs.getInt("id_pedido"), rs.getObject("fecha_pago", LocalDateTime.class),
                        EstadoPedido.valueOf(rs.getString("estado_pedido")), rs.getInt("cantidad_total_libros"), rs.getBigDecimal("total"));
                return new DetalleDevolucionDTO(mapearResumen(rs),
                        new DatosDevolucionDTO(MotivoDevolucion.valueOf(rs.getString("motivo_devolucion")), rs.getString("comentario")),
                        pedido, rs.getObject("fecha_resolucion", LocalDateTime.class));
            }
        }
    }

    @Override
    public DetalleReporteResenaDTO obtenerDetalleReporteResena(int id, boolean incluirContenidoActual) throws SQLException {
        // La consulta propia usa solo el contenido original. Moderación pide además el actual.
        String actuales = incluirContenidoActual ? "r.puntuacion AS puntuacion_actual, r.comentario AS comentario_actual"
                : "NULL AS puntuacion_actual, NULL AS comentario_actual";
        String sql = "SELECT s.*, u.nombre AS nombre_solicitante, autor.nombre AS nombre_autor, l.titulo, " + actuales
                + ", (r.activo AND NOT r.eliminado) AS resena_activa FROM SOLICITUD s JOIN USUARIO u ON u.id = s.id_usuario"
                + " JOIN RESENA r ON r.id = s.id_resena JOIN USUARIO autor ON autor.id = r.id_usuario JOIN LIBRO l ON l.id = r.id_libro"
                + " WHERE s.id = ? AND s.tipo_solicitud = 'REPORTE_RESENA'";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                ResenaReportadaDTO referencia = new ResenaReportadaDTO(rs.getInt("id_resena"), rs.getString("nombre_autor"), rs.getString("titulo"),
                        new DatosResenaDTO(rs.getInt("puntuacion_original"), rs.getString("comentario_original")));
                DatosResenaDTO actual = incluirContenidoActual ? new DatosResenaDTO(rs.getInt("puntuacion_actual"), rs.getString("comentario_actual")) : null;
                return new DetalleReporteResenaDTO(mapearResumen(rs),
                        new DatosReporteResenaDTO(MotivoReporteResena.valueOf(rs.getString("motivo_reporte")), rs.getString("comentario")),
                        referencia, actual, rs.getBoolean("resena_activa"), rs.getObject("fecha_resolucion", LocalDateTime.class));
            }
        }
    }

    @Override
    public boolean existeDevolucionPendiente(int idPedido) throws SQLException {
        String sql = "SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'DEVOLUCION' AND id_pedido = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idPedido);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean existeReportePendiente(int idUsuario, int idResena) throws SQLException {
        String sql = "SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'REPORTE_RESENA' AND id_usuario = ? AND id_resena = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idResena);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean resolver(int id, EstadoSolicitud estado, LocalDateTime fecha, String actor) throws SQLException {
        if (estado == null || estado == EstadoSolicitud.PENDIENTE || fecha == null)
            throw new IllegalArgumentException("La resolucion requiere un resultado y una fecha.");
        String sql = """
                UPDATE SOLICITUD SET estado = ?, fecha_resolucion = ?, usuario_actualizacion = ?, fecha_actualizacion = ?
                WHERE id = ? AND estado = 'PENDIENTE' AND NOT eliminado
                    AND (? <> 'CERRADA' OR tipo_solicitud = 'REPORTE_RESENA')
                """;
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, estado.name());
            statement.setTimestamp(2, Timestamp.valueOf(fecha));
            statement.setString(3, actor);
            statement.setTimestamp(4, Timestamp.valueOf(fecha));
            statement.setInt(5, id);
            statement.setString(6, estado.name());
            return statement.executeUpdate() == 1;
        }
    }
}
