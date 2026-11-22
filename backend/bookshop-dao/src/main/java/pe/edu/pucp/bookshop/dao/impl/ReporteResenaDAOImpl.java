package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.ReporteResenaDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.EstadoSolicitud;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.MotivoReporte;
import pe.edu.pucp.bookshop.model.ReporteResena;
import pe.edu.pucp.bookshop.model.Resena;
import pe.edu.pucp.bookshop.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteResenaDAOImpl implements ReporteResenaDAO {

    @Override
    public List<ReporteResena> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select rr.id_reporte_resena, rr.motivo, rr.comentario, rr.contenido_reportado, rr.puntuacion_reportada,
                   rr.estado, rr.fecha,
                   r.id_resena, a.nombre_completo as autor_resena,
                   l.id_libro, l.titulo, l.portada
            from reporte_resena rr
            join resena r on r.id_resena = rr.fid_resena
            join usuario a on a.id_usuario = r.fid_usuario
            join libro l on l.id_libro = r.fid_libro
            where rr.fid_usuario = ?
            order by rr.fecha desc
        """;
        List<ReporteResena> reportes = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    ReporteResena reporte = new ReporteResena();
                    reporte.setId(rs.getInt("id_reporte_resena"));
                    reporte.setMotivo(MotivoReporte.valueOf(rs.getString("motivo")));
                    reporte.setComentario(rs.getString("comentario"));
                    reporte.setContenidoReportado(rs.getString("contenido_reportado"));
                    reporte.setPuntuacionReportada(rs.getInt("puntuacion_reportada"));
                    reporte.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                    reporte.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    Resena resena = new Resena();
                    resena.setId(rs.getInt("id_resena"));
                    Usuario autor = new Usuario();
                    autor.setNombreCompleto(rs.getString("autor_resena"));
                    resena.setUsuario(autor);
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    resena.setLibro(libro);
                    reporte.setResena(resena);
                    reportes.add(reporte);
                }
            }
        }
        return reportes;
    }

    // Las más antiguas primero
    @Override
    public List<ReporteResena> listarPorEstado(EstadoSolicitud estado) throws SQLException {
        String sql = """
            select rr.id_reporte_resena, rr.motivo, rr.comentario, rr.contenido_reportado, rr.puntuacion_reportada,
                   rr.estado, rr.fecha, u.nombre_completo as reportante,
                   r.id_resena, r.fecha as fecha_resena, a.nombre_completo as autor_resena,
                   l.id_libro, l.titulo, l.portada
            from reporte_resena rr
            join usuario u on u.id_usuario = rr.fid_usuario
            join resena r on r.id_resena = rr.fid_resena
            join usuario a on a.id_usuario = r.fid_usuario
            join libro l on l.id_libro = r.fid_libro
            where rr.estado = ?
            order by rr.fecha
        """;
        List<ReporteResena> reportes = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, estado.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    ReporteResena reporte = new ReporteResena();
                    reporte.setId(rs.getInt("id_reporte_resena"));
                    reporte.setMotivo(MotivoReporte.valueOf(rs.getString("motivo")));
                    reporte.setComentario(rs.getString("comentario"));
                    reporte.setContenidoReportado(rs.getString("contenido_reportado"));
                    reporte.setPuntuacionReportada(rs.getInt("puntuacion_reportada"));
                    reporte.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                    reporte.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    Usuario reportante = new Usuario();
                    reportante.setNombreCompleto(rs.getString("reportante"));
                    reporte.setUsuario(reportante);
                    Resena resena = new Resena();
                    resena.setId(rs.getInt("id_resena"));
                    resena.setFecha(rs.getObject("fecha_resena", LocalDateTime.class));
                    Usuario autor = new Usuario();
                    autor.setNombreCompleto(rs.getString("autor_resena"));
                    resena.setUsuario(autor);
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    resena.setLibro(libro);
                    reporte.setResena(resena);
                    reportes.add(reporte);
                }
            }
        }
        return reportes;
    }

    @Override
    public int contarPorUsuarioYResena(int idUsuario, int idResena, EstadoSolicitud estado) throws SQLException {
        String sql = "select count(*) from reporte_resena where fid_usuario = ? and fid_resena = ? and estado = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idResena);
            pstm.setString(3, estado.name());
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int insertar(ReporteResena reporte) throws SQLException {
        String sql = """
            insert into reporte_resena (fid_usuario, fid_resena, motivo, comentario, contenido_reportado, puntuacion_reportada, estado, fecha)
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, reporte.getUsuario().getId());
            pstm.setInt(2, reporte.getResena().getId());
            pstm.setString(3, reporte.getMotivo().name());
            pstm.setString(4, reporte.getComentario());
            pstm.setString(5, reporte.getContenidoReportado());
            pstm.setInt(6, reporte.getPuntuacionReportada());
            pstm.setString(7, reporte.getEstado().name());
            pstm.setObject(8, reporte.getFecha());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void modificar(ReporteResena reporte) throws SQLException {
        String sql = "update reporte_resena set motivo = ?, comentario = ? where id_reporte_resena = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, reporte.getMotivo().name());
            pstm.setString(2, reporte.getComentario());
            pstm.setInt(3, reporte.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarEstado(int id, EstadoSolicitud estado) throws SQLException {
        String sql = "update reporte_resena set estado = ? where id_reporte_resena = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, estado.name());
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminarPorResena(int idResena) throws SQLException {
        String sql = "delete from reporte_resena where fid_resena = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idResena);
            pstm.executeUpdate();
        }
    }
}
