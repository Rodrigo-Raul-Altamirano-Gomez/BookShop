package me.DAO.catalogo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.opinion.*;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.opinion.Resena;
import me.Model.entities.usuario.Usuario;

public class ResenaDAOImpl implements ResenaDAO {
    @Override
    public void insertar(Resena resena) throws SQLException {
        try (Connection conexion = DBManager.getConnection()) { insertar(conexion, resena); }
    }

    @Override
    public void insertarEnTransaccion(Resena resena) throws SQLException {
        insertar(TransactionContext.getConnection(), resena);
    }

    private void insertar(Connection conexion, Resena resena) throws SQLException {
        String sql = """
                INSERT INTO RESENA (puntuacion, comentario, fecha, id_libro, id_usuario,
                    usuario_creacion, fecha_creacion, activo, eliminado, cantidad_votos_utiles)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, resena.getPuntuacion());
            statement.setString(2, resena.getComentario());
            statement.setTimestamp(3, Timestamp.valueOf(resena.getFecha()));
            statement.setInt(4, resena.getLibro().getId());
            statement.setInt(5, resena.getUsuario().getId());
            statement.setString(6, resena.getUsuarioCreacion());
            statement.setTimestamp(7, Timestamp.valueOf(resena.getFechaCreacion()));
            statement.setBoolean(8, resena.isActivo());
            statement.setBoolean(9, resena.isEliminado());
            statement.setInt(10, resena.getCantidadVotosUtiles());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No se genero el identificador de la resena.");
                resena.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Resena resena) throws SQLException {
        // Editar contenido no cambia libro, propietario, publicación ni contador.
        String sql = """
                UPDATE RESENA SET puntuacion = ?, comentario = ?, usuario_actualizacion = ?,
                    fecha_actualizacion = ? WHERE id = ? AND activo AND NOT eliminado
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, resena.getPuntuacion());
            statement.setString(2, resena.getComentario());
            statement.setString(3, resena.getUsuarioActualizacion());
            statement.setTimestamp(4, Timestamp.valueOf(resena.getFechaActualizacion() == null ? LocalDateTime.now() : resena.getFechaActualizacion()));
            statement.setInt(5, resena.getId());
            if (statement.executeUpdate() != 1) throw new SQLException("La resena no puede modificarse.");
        }
    }

    @Override
    public Resena leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection()) { return leerRegistro(conexion, id, false); }
    }

    @Override
    public Resena leerParaActualizar(int id) throws SQLException {
        return leerRegistro(TransactionContext.getConnection(), id, true);
    }

    private Resena leerRegistro(Connection conexion, int id, boolean bloquear) throws SQLException {
        try (PreparedStatement statement = conexion.prepareStatement("SELECT * FROM RESENA WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapearResena(rs) : null; }
        }
    }

    @Override
    public boolean eliminar(int id, String actor) throws SQLException {
        String sql = "UPDATE RESENA SET eliminado = TRUE, activo = FALSE, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, actor);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    private Resena mapearResena(ResultSet rs) throws SQLException {
        Resena resena = new Resena();
        resena.setId(rs.getInt("id"));
        Libro libro = new Libro();
        libro.setId(rs.getInt("id_libro"));
        resena.setLibro(libro);
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario"));
        resena.setUsuario(usuario);
        resena.setPuntuacion(rs.getInt("puntuacion"));
        resena.setComentario(rs.getString("comentario"));
        resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
        resena.setActivo(rs.getBoolean("activo"));
        resena.setEliminado(rs.getBoolean("eliminado"));
        resena.setCantidadVotosUtiles(rs.getInt("cantidad_votos_utiles"));
        resena.setUsuarioCreacion(rs.getString("usuario_creacion"));
        resena.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
        resena.setUsuarioActualizacion(rs.getString("usuario_actualizacion"));
        resena.setFechaActualizacion(rs.getObject("fecha_actualizacion", LocalDateTime.class));
        return resena;
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listar(int idLibro, int idUsuario, int idUsuarioActual, ConsultaResenasDTO consulta) throws SQLException {
        if (consulta == null) consulta = new ConsultaResenasDTO();
        if (consulta.pagina() < 1 || consulta.tamanioPagina() < 1) throw new IllegalArgumentException("Pagina y tamanio deben ser positivos.");
        // Cero/cero representa Todas las reseñas; Business autoriza ese ámbito.
        String condiciones = " WHERE NOT r.eliminado";
        if (idLibro > 0) condiciones += " AND r.id_libro = ? AND r.activo";
        if (idUsuario > 0) condiciones += " AND r.id_usuario = ?";
        OrdenResena orden = consulta.orden() == null ? OrdenResena.MAS_RECIENTES : consulta.orden();
        String ordenar = switch (orden) {
            case VOTOS_UTILES_ASCENDENTE -> "r.cantidad_votos_utiles ASC, r.fecha DESC, r.id DESC";
            case VOTOS_UTILES_DESCENDENTE -> "r.cantidad_votos_utiles DESC, r.fecha DESC, r.id DESC";
            case PUNTUACION_ASCENDENTE -> "r.puntuacion ASC, r.fecha DESC, r.id DESC";
            case PUNTUACION_DESCENDENTE -> "r.puntuacion DESC, r.fecha DESC, r.id DESC";
            case MAS_RECIENTES -> "r.fecha DESC, r.id DESC";
        };
        boolean mostrarPortada = idLibro == 0;
        boolean mostrarUsuario = idUsuario == 0;
        String imagenes = (mostrarPortada ? "l.portada, l.tipo_portada" : "NULL AS portada, NULL AS tipo_portada")
                + ", " + (mostrarUsuario ? "u.imagen, u.tipo_imagen" : "NULL AS imagen, NULL AS tipo_imagen");
        String sql = "SELECT r.*, l.titulo, u.nombre, " + imagenes
                + ", EXISTS(SELECT 1 FROM VOTO_UTIL v WHERE v.id_resena = r.id AND v.id_usuario = ? AND NOT v.eliminado) AS voto_propio"
                + " FROM RESENA r JOIN LIBRO l ON l.id = r.id_libro JOIN USUARIO u ON u.id = r.id_usuario"
                + condiciones + " ORDER BY " + ordenar + " LIMIT ? OFFSET ?";
        List<ResenaDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*) FROM RESENA r" + condiciones)) {
                int parametro = 1;
                if (idLibro > 0) statement.setInt(parametro++, idLibro);
                if (idUsuario > 0) statement.setInt(parametro, idUsuario);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                int parametro = 1;
                statement.setInt(parametro++, idLibro > 0 && idUsuario == 0 ? idUsuarioActual : 0);
                if (idLibro > 0) statement.setInt(parametro++, idLibro);
                if (idUsuario > 0) statement.setInt(parametro++, idUsuario);
                statement.setInt(parametro++, consulta.tamanioPagina());
                statement.setLong(parametro, (long) (consulta.pagina() - 1) * consulta.tamanioPagina());
                try (ResultSet rs = statement.executeQuery()) { while (rs.next()) items.add(mapearResenaDTO(rs)); }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, consulta.pagina(), consulta.tamanioPagina());
    }

    @Override
    public ResenaDTO obtenerReferencia(int id, boolean incluirImagenes) throws SQLException {
        String imagenes = incluirImagenes ? "l.portada, l.tipo_portada, u.imagen, u.tipo_imagen" : "NULL AS portada, NULL AS tipo_portada, NULL AS imagen, NULL AS tipo_imagen";
        String sql = "SELECT r.*, l.titulo, u.nombre, " + imagenes + ", FALSE AS voto_propio"
                + " FROM RESENA r JOIN LIBRO l ON l.id = r.id_libro JOIN USUARIO u ON u.id = r.id_usuario WHERE r.id = ?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapearResenaDTO(rs) : null; }
        }
    }

    private ResenaDTO mapearResenaDTO(ResultSet rs) throws SQLException {
        byte[] portada = rs.getBytes("portada");
        byte[] imagen = rs.getBytes("imagen");
        return new ResenaDTO(rs.getInt("id"), rs.getInt("id_libro"), rs.getString("titulo"),
                portada == null ? null : new ImagenDTO(portada, rs.getString("tipo_portada")),
                rs.getString("nombre"), imagen == null ? null : new ImagenDTO(imagen, rs.getString("tipo_imagen")),
                rs.getInt("puntuacion"), rs.getString("comentario"), rs.getObject("fecha", LocalDateTime.class),
                rs.getInt("cantidad_votos_utiles"), rs.getBoolean("voto_propio"), rs.getBoolean("activo") && !rs.getBoolean("eliminado"));
    }

    @Override
    public boolean existePublicada(int idUsuario, int idLibro) throws SQLException {
        String sql = "SELECT 1 FROM RESENA WHERE id_usuario = ? AND id_libro = ? AND activo AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idLibro);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public ValoracionLibroDTO obtenerValoracion(int idLibro) throws SQLException {
        String sql = """
                SELECT SUM(CASE WHEN puntuacion = 1 THEN 1 ELSE 0 END) AS una,
                    SUM(CASE WHEN puntuacion = 2 THEN 1 ELSE 0 END) AS dos,
                    SUM(CASE WHEN puntuacion = 3 THEN 1 ELSE 0 END) AS tres,
                    SUM(CASE WHEN puntuacion = 4 THEN 1 ELSE 0 END) AS cuatro,
                    SUM(CASE WHEN puntuacion = 5 THEN 1 ELSE 0 END) AS cinco
                FROM RESENA WHERE id_libro = ? AND activo AND NOT eliminado
                """;
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idLibro);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return new ValoracionLibroDTO(idLibro, rs.getLong("una"), rs.getLong("dos"), rs.getLong("tres"), rs.getLong("cuatro"), rs.getLong("cinco"));
            }
        }
    }

    @Override
    public List<ValoracionLibroDTO> listarValoraciones() throws SQLException {
        String sql = """
                SELECT id_libro, SUM(CASE WHEN puntuacion = 1 THEN 1 ELSE 0 END) AS una,
                    SUM(CASE WHEN puntuacion = 2 THEN 1 ELSE 0 END) AS dos,
                    SUM(CASE WHEN puntuacion = 3 THEN 1 ELSE 0 END) AS tres,
                    SUM(CASE WHEN puntuacion = 4 THEN 1 ELSE 0 END) AS cuatro,
                    SUM(CASE WHEN puntuacion = 5 THEN 1 ELSE 0 END) AS cinco
                FROM RESENA WHERE activo AND NOT eliminado GROUP BY id_libro ORDER BY id_libro
                """;
        List<ValoracionLibroDTO> items = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement statement = conexion.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) items.add(new ValoracionLibroDTO(rs.getInt("id_libro"), rs.getLong("una"), rs.getLong("dos"), rs.getLong("tres"), rs.getLong("cuatro"), rs.getLong("cinco")));
        }
        return items;
    }
}

