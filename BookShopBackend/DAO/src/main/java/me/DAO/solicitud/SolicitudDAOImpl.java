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
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT id FROM PEDIDO WHERE id = ? AND id_usuario = ? FOR UPDATE")) {
                sentencia.setInt(1, devolucion.getPedido().getId());
                sentencia.setInt(2, devolucion.getUsuario().getId());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) throw new SQLException("El pedido de la devolucion no existe para este usuario.");
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'DEVOLUCION' AND id_pedido = ? AND estado = 'PENDIENTE' AND NOT eliminado")) {
                sentencia.setInt(1, devolucion.getPedido().getId());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (resultado.next()) throw new SQLException("Ya existe una devolucion pendiente para el pedido.");
                }
            }
        } else if (solicitud instanceof SolicitudReporteResena reporte) {
            Resena original = new ResenaDAOImpl().leerParaActualizar(reporte.getResena().getId());
            if (original == null) throw new SQLException("La resena reportada no existe.");
            reporte.setPuntuacionOriginal(original.getPuntuacion());
            reporte.setComentarioOriginal(original.getComentario());
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'REPORTE_RESENA' AND id_usuario = ? AND id_resena = ? AND estado = 'PENDIENTE' AND NOT eliminado")) {
                sentencia.setInt(1, reporte.getUsuario().getId());
                sentencia.setInt(2, reporte.getResena().getId());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (resultado.next()) throw new SQLException("Ya existe un reporte pendiente del usuario para la resena.");
                }
            }
        }
        String sql = """
                INSERT INTO SOLICITUD (tipo_solicitud, id_usuario, id_pedido, motivo_devolucion,
                    id_resena, motivo_reporte, puntuacion_original, comentario_original,
                    comentario, estado, fecha_resolucion, eliminado, usuario_creacion, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', NULL, FALSE, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, tipo.name());
            sentencia.setInt(2, solicitud.getUsuario().getId());
            if (solicitud instanceof SolicitudDevolucion devolucion) {
                sentencia.setInt(3, devolucion.getPedido().getId());
                sentencia.setString(4, devolucion.getMotivo().name());
                sentencia.setNull(5, Types.INTEGER);
                sentencia.setNull(6, Types.VARCHAR);
                sentencia.setNull(7, Types.TINYINT);
                sentencia.setNull(8, Types.LONGVARCHAR);
            } else {
                SolicitudReporteResena reporte = (SolicitudReporteResena) solicitud;
                sentencia.setNull(3, Types.INTEGER);
                sentencia.setNull(4, Types.VARCHAR);
                sentencia.setInt(5, reporte.getResena().getId());
                sentencia.setString(6, reporte.getMotivo().name());
                sentencia.setInt(7, reporte.getPuntuacionOriginal());
                sentencia.setString(8, reporte.getComentarioOriginal());
            }
            sentencia.setString(9, solicitud.getComentario());
            sentencia.setString(10, solicitud.getUsuarioCreacion());
            sentencia.setTimestamp(11, Timestamp.valueOf(solicitud.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet resultado = sentencia.getGeneratedKeys()) {
                if (!resultado.next()) throw new SQLException("No se genero el identificador de la solicitud.");
                solicitud.setId(resultado.getInt(1));
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, motivo);
            sentencia.setString(2, solicitud.getComentario());
            sentencia.setString(3, solicitud.getUsuarioActualizacion());
            sentencia.setTimestamp(4, Timestamp.valueOf(solicitud.getFechaActualizacion() == null ? LocalDateTime.now() : solicitud.getFechaActualizacion()));
            sentencia.setInt(5, solicitud.getId());
            sentencia.setString(6, tipo.name());
            if (sentencia.executeUpdate() != 1) throw new SQLException("Solo puede editarse una solicitud pendiente.");
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
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM SOLICITUD WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next() ? mapearSolicitud(resultado) : null; }
        }
    }

    private Solicitud mapearSolicitud(ResultSet resultado) throws SQLException {
        Solicitud solicitud;
        if (TipoSolicitud.valueOf(resultado.getString("tipo_solicitud")) == TipoSolicitud.DEVOLUCION) {
            SolicitudDevolucion devolucion = new SolicitudDevolucion();
            Pedido pedido = new Pedido();
            pedido.setId(resultado.getInt("id_pedido"));
            devolucion.setPedido(pedido);
            devolucion.setMotivo(MotivoDevolucion.valueOf(resultado.getString("motivo_devolucion")));
            solicitud = devolucion;
        } else {
            SolicitudReporteResena reporte = new SolicitudReporteResena();
            Resena resena = new Resena();
            resena.setId(resultado.getInt("id_resena"));
            reporte.setResena(resena);
            reporte.setMotivo(MotivoReporteResena.valueOf(resultado.getString("motivo_reporte")));
            reporte.setPuntuacionOriginal(resultado.getInt("puntuacion_original"));
            reporte.setComentarioOriginal(resultado.getString("comentario_original"));
            solicitud = reporte;
        }
        solicitud.setId(resultado.getInt("id"));
        Usuario usuario = new Usuario();
        usuario.setId(resultado.getInt("id_usuario"));
        solicitud.setUsuario(usuario);
        solicitud.setComentario(resultado.getString("comentario"));
        solicitud.setEstado(EstadoSolicitud.valueOf(resultado.getString("estado")));
        solicitud.setFechaResolucion(resultado.getObject("fecha_resolucion", LocalDateTime.class));
        solicitud.setEliminado(resultado.getBoolean("eliminado"));
        solicitud.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        solicitud.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        solicitud.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        solicitud.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return solicitud;
    }

    @Override
    public boolean eliminar(int id, String actor) throws SQLException {
        String sql = "UPDATE SOLICITUD SET eliminado = TRUE, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, actor);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<ResumenSolicitudDTO> listar(int idUsuario, FiltroSolicitudDTO filtro) throws SQLException {
        if (filtro == null) filtro = FiltroSolicitudDTO.porDefecto();
        String condiciones = " WHERE s.tipo_solicitud = ? AND NOT s.eliminado";
        if (idUsuario > 0) condiciones += " AND s.id_usuario = ?";
        if (!filtro.estados().isEmpty()) condiciones += " AND s.estado IN (" + String.join(",", java.util.Collections.nCopies(filtro.estados().size(), "?")) + ")";
        String sql = "SELECT s.*, u.nombre AS nombre_solicitante FROM SOLICITUD s JOIN USUARIO u ON u.id = s.id_usuario"
                + condiciones + " ORDER BY s.fecha_creacion DESC, s.id DESC LIMIT ? OFFSET ?";
        List<ResumenSolicitudDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM SOLICITUD s" + condiciones)) {
                int parametro = 1;
                sentencia.setString(parametro++, filtro.tipo().name());
                if (idUsuario > 0) sentencia.setInt(parametro++, idUsuario);
                for (EstadoSolicitud estado : filtro.estados()) sentencia.setString(parametro++, estado.name());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = 1;
                sentencia.setString(parametro++, filtro.tipo().name());
                if (idUsuario > 0) sentencia.setInt(parametro++, idUsuario);
                for (EstadoSolicitud estado : filtro.estados()) sentencia.setString(parametro++, estado.name());
                sentencia.setInt(parametro++, filtro.tamanioPagina());
                sentencia.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) { while (resultado.next()) items.add(mapearResumen(resultado)); }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    private ResumenSolicitudDTO mapearResumen(ResultSet resultado) throws SQLException {
        TipoSolicitud tipo = TipoSolicitud.valueOf(resultado.getString("tipo_solicitud"));
        String motivo;
        if (tipo == TipoSolicitud.DEVOLUCION) {
            motivo = switch (MotivoDevolucion.valueOf(resultado.getString("motivo_devolucion"))) {
                case LIBRO_DANADO -> "Libro dañado";
                case LIBRO_INCORRECTO -> "Libro incorrecto";
                case PEDIDO_INCOMPLETO -> "Pedido incompleto";
                case NO_CUMPLE_EXPECTATIVAS -> "No cumple expectativas";
                case OTRO -> "Otro";
            };
        } else {
            motivo = switch (MotivoReporteResena.valueOf(resultado.getString("motivo_reporte"))) {
                case CONTENIDO_OFENSIVO -> "Contenido ofensivo";
                case PUBLICIDAD_O_SPAM -> "Publicidad o spam";
                case CONTENIDO_AJENO_AL_LIBRO -> "Contenido ajeno al libro";
                case OTRO -> "Otro";
            };
        }
        return new ResumenSolicitudDTO(resultado.getInt("id"), tipo, resultado.getObject("fecha_creacion", LocalDateTime.class),
                EstadoSolicitud.valueOf(resultado.getString("estado")), resultado.getString("nombre_solicitante"), motivo,
                resultado.getInt(tipo == TipoSolicitud.DEVOLUCION ? "id_pedido" : "id_resena"));
    }

    @Override
    public DetalleDevolucionDTO obtenerDetalleDevolucion(int id) throws SQLException {
        String sql = """
                SELECT s.*, u.nombre AS nombre_solicitante, p.fecha_pago, p.estado AS estado_pedido,
                    p.cantidad_total_libros, p.total
                FROM SOLICITUD s JOIN USUARIO u ON u.id = s.id_usuario JOIN PEDIDO p ON p.id = s.id_pedido
                WHERE s.id = ? AND s.tipo_solicitud = 'DEVOLUCION'
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                ResumenPedidoDTO pedido = new ResumenPedidoDTO(resultado.getInt("id_pedido"), resultado.getObject("fecha_pago", LocalDateTime.class),
                        EstadoPedido.valueOf(resultado.getString("estado_pedido")), resultado.getInt("cantidad_total_libros"), resultado.getBigDecimal("total"));
                return new DetalleDevolucionDTO(mapearResumen(resultado),
                        new DatosDevolucionDTO(MotivoDevolucion.valueOf(resultado.getString("motivo_devolucion")), resultado.getString("comentario")),
                        pedido, resultado.getObject("fecha_resolucion", LocalDateTime.class));
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                ResenaReportadaDTO referencia = new ResenaReportadaDTO(resultado.getInt("id_resena"), resultado.getString("nombre_autor"), resultado.getString("titulo"),
                        new DatosResenaDTO(resultado.getInt("puntuacion_original"), resultado.getString("comentario_original")));
                DatosResenaDTO actual = incluirContenidoActual ? new DatosResenaDTO(resultado.getInt("puntuacion_actual"), resultado.getString("comentario_actual")) : null;
                return new DetalleReporteResenaDTO(mapearResumen(resultado),
                        new DatosReporteResenaDTO(MotivoReporteResena.valueOf(resultado.getString("motivo_reporte")), resultado.getString("comentario")),
                        referencia, actual, resultado.getBoolean("resena_activa"), resultado.getObject("fecha_resolucion", LocalDateTime.class));
            }
        }
    }

    @Override
    public boolean existeDevolucionPendiente(int idPedido) throws SQLException {
        String sql = "SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'DEVOLUCION' AND id_pedido = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idPedido);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next(); }
        }
    }

    @Override
    public boolean existeReportePendiente(int idUsuario, int idResena) throws SQLException {
        String sql = "SELECT 1 FROM SOLICITUD WHERE tipo_solicitud = 'REPORTE_RESENA' AND id_usuario = ? AND id_resena = ? AND estado = 'PENDIENTE' AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idResena);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next(); }
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estado.name());
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            sentencia.setString(3, actor);
            sentencia.setTimestamp(4, Timestamp.valueOf(fecha));
            sentencia.setInt(5, id);
            sentencia.setString(6, estado.name());
            return sentencia.executeUpdate() == 1;
        }
    }
}
