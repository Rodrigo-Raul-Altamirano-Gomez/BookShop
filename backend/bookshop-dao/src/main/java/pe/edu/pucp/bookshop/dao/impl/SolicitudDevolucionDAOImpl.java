package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.SolicitudDevolucionDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.DetallePedido;
import pe.edu.pucp.bookshop.model.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.MotivoDevolucion;
import pe.edu.pucp.bookshop.model.Pedido;
import pe.edu.pucp.bookshop.model.SolicitudDevolucion;
import pe.edu.pucp.bookshop.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SolicitudDevolucionDAOImpl implements SolicitudDevolucionDAO {

    // Una fila por cada libro del pedido de cada solicitud; las más recientes primero
    @Override
    public List<SolicitudDevolucion> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select s.id_solicitud_devolucion, s.motivo, s.comentario, s.estado, s.fecha,
                   p.id_pedido, d.cantidad, l.id_libro, l.titulo, l.portada
            from solicitud_devolucion s
            join pedido p on p.id_pedido = s.fid_pedido
            join detalle_pedido d on d.fid_pedido = p.id_pedido
            join libro l on l.id_libro = d.fid_libro
            where p.fid_usuario = ?
            order by s.fecha desc, s.id_solicitud_devolucion, d.id_detalle_pedido
        """;
        List<SolicitudDevolucion> solicitudes = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                SolicitudDevolucion solicitud = null;
                while (rs.next()) {
                    if (solicitud == null || solicitud.getId() != rs.getInt("id_solicitud_devolucion")) {
                        solicitud = new SolicitudDevolucion();
                        solicitud.setId(rs.getInt("id_solicitud_devolucion"));
                        solicitud.setMotivo(MotivoDevolucion.valueOf(rs.getString("motivo")));
                        solicitud.setComentario(rs.getString("comentario"));
                        solicitud.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                        solicitud.setFecha(rs.getObject("fecha", LocalDateTime.class));
                        Pedido pedido = new Pedido();
                        pedido.setId(rs.getInt("id_pedido"));
                        solicitud.setPedido(pedido);
                        solicitudes.add(solicitud);
                    }
                    DetallePedido detalle = new DetallePedido();
                    detalle.setCantidad(rs.getInt("cantidad"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    detalle.setLibro(libro);
                    solicitud.getPedido().getDetalles().add(detalle);
                }
            }
        }
        return solicitudes;
    }

    // Una fila por cada libro del pedido de cada solicitud; las más antiguas primero
    @Override
    public List<SolicitudDevolucion> listarPorEstado(EstadoSolicitud estado) throws SQLException {
        String sql = """
            select s.id_solicitud_devolucion, s.motivo, s.comentario, s.estado, s.fecha,
                   p.id_pedido, p.fecha_entrega, p.total,
                   u.id_usuario, u.nombre_completo, u.correo,
                   d.cantidad, l.id_libro, l.titulo, l.portada
            from solicitud_devolucion s
            join pedido p on p.id_pedido = s.fid_pedido
            join usuario u on u.id_usuario = p.fid_usuario
            join detalle_pedido d on d.fid_pedido = p.id_pedido
            join libro l on l.id_libro = d.fid_libro
            where s.estado = ?
            order by s.fecha, s.id_solicitud_devolucion, d.id_detalle_pedido
        """;
        List<SolicitudDevolucion> solicitudes = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, estado.name());
            try (ResultSet rs = pstm.executeQuery()) {
                SolicitudDevolucion solicitud = null;
                while (rs.next()) {
                    if (solicitud == null || solicitud.getId() != rs.getInt("id_solicitud_devolucion")) {
                        solicitud = new SolicitudDevolucion();
                        solicitud.setId(rs.getInt("id_solicitud_devolucion"));
                        solicitud.setMotivo(MotivoDevolucion.valueOf(rs.getString("motivo")));
                        solicitud.setComentario(rs.getString("comentario"));
                        solicitud.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                        solicitud.setFecha(rs.getObject("fecha", LocalDateTime.class));
                        Pedido pedido = new Pedido();
                        pedido.setId(rs.getInt("id_pedido"));
                        pedido.setFechaEntrega(rs.getObject("fecha_entrega", LocalDateTime.class));
                        pedido.setTotal(rs.getDouble("total"));
                        Usuario usuario = new Usuario();
                        usuario.setId(rs.getInt("id_usuario"));
                        usuario.setNombreCompleto(rs.getString("nombre_completo"));
                        usuario.setCorreo(rs.getString("correo"));
                        pedido.setUsuario(usuario);
                        solicitud.setPedido(pedido);
                        solicitudes.add(solicitud);
                    }
                    DetallePedido detalle = new DetallePedido();
                    detalle.setCantidad(rs.getInt("cantidad"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    detalle.setLibro(libro);
                    solicitud.getPedido().getDetalles().add(detalle);
                }
            }
        }
        return solicitudes;
    }

    @Override
    public List<SolicitudDevolucion> listarPorPedido(int idPedido) throws SQLException {
        String sql = """
            select id_solicitud_devolucion, motivo, comentario, estado, fecha
            from solicitud_devolucion
            where fid_pedido = ?
        """;
        List<SolicitudDevolucion> solicitudes = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idPedido);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    SolicitudDevolucion solicitud = new SolicitudDevolucion();
                    solicitud.setId(rs.getInt("id_solicitud_devolucion"));
                    solicitud.setMotivo(MotivoDevolucion.valueOf(rs.getString("motivo")));
                    solicitud.setComentario(rs.getString("comentario"));
                    solicitud.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                    solicitud.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    solicitudes.add(solicitud);
                }
            }
        }
        return solicitudes;
    }

    @Override
    public int insertar(SolicitudDevolucion solicitud) throws SQLException {
        String sql = "insert into solicitud_devolucion (fid_pedido, motivo, comentario, estado, fecha) values (?, ?, ?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, solicitud.getPedido().getId());
            pstm.setString(2, solicitud.getMotivo().name());
            pstm.setString(3, solicitud.getComentario());
            pstm.setString(4, solicitud.getEstado().name());
            pstm.setObject(5, solicitud.getFecha());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(SolicitudDevolucion solicitud) throws SQLException {
        String sql = "update solicitud_devolucion set motivo = ?, comentario = ? where id_solicitud_devolucion = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, solicitud.getMotivo().name());
            pstm.setString(2, solicitud.getComentario());
            pstm.setInt(3, solicitud.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarEstado(int id, EstadoSolicitud estado) throws SQLException {
        String sql = "update solicitud_devolucion set estado = ? where id_solicitud_devolucion = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, estado.name());
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }
}
