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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, resena.getPuntuacion());
            sentencia.setString(2, resena.getComentario());
            sentencia.setTimestamp(3, Timestamp.valueOf(resena.getFecha()));
            sentencia.setInt(4, resena.getLibro().getId());
            sentencia.setInt(5, resena.getUsuario().getId());
            sentencia.setString(6, resena.getUsuarioCreacion());
            sentencia.setTimestamp(7, Timestamp.valueOf(resena.getFechaCreacion()));
            sentencia.setBoolean(8, resena.isActivo());
            sentencia.setBoolean(9, resena.isEliminado());
            sentencia.setInt(10, resena.getCantidadVotosUtiles());
            sentencia.executeUpdate();
            try (ResultSet resultado = sentencia.getGeneratedKeys()) {
                if (!resultado.next()) throw new SQLException("No se genero el identificador de la resena.");
                resena.setId(resultado.getInt(1));
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, resena.getPuntuacion());
            sentencia.setString(2, resena.getComentario());
            sentencia.setString(3, resena.getUsuarioActualizacion());
            sentencia.setTimestamp(4, Timestamp.valueOf(resena.getFechaActualizacion() == null ? LocalDateTime.now() : resena.getFechaActualizacion()));
            sentencia.setInt(5, resena.getId());
            if (sentencia.executeUpdate() != 1) throw new SQLException("La resena no puede modificarse.");
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
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM RESENA WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next() ? mapearResena(resultado) : null; }
        }
    }

    @Override
    public boolean eliminar(int id, String actor) throws SQLException {
        String sql = "UPDATE RESENA SET eliminado = TRUE, activo = FALSE, usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, actor);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    private Resena mapearResena(ResultSet resultado) throws SQLException {
        Resena resena = new Resena();
        resena.setId(resultado.getInt("id"));
        Libro libro = new Libro();
        libro.setId(resultado.getInt("id_libro"));
        resena.setLibro(libro);
        Usuario usuario = new Usuario();
        usuario.setId(resultado.getInt("id_usuario"));
        resena.setUsuario(usuario);
        resena.setPuntuacion(resultado.getInt("puntuacion"));
        resena.setComentario(resultado.getString("comentario"));
        resena.setFecha(resultado.getObject("fecha", LocalDateTime.class));
        resena.setActivo(resultado.getBoolean("activo"));
        resena.setEliminado(resultado.getBoolean("eliminado"));
        resena.setCantidadVotosUtiles(resultado.getInt("cantidad_votos_utiles"));
        resena.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        resena.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        resena.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        resena.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return resena;
    }

    @Override
    public ResultadoPaginadoDTO<ResenaDTO> listar(int idLibro, int idUsuario, int idUsuarioActual, ConsultaResenasDTO consulta) throws SQLException {
        if (consulta == null) consulta = ConsultaResenasDTO.porDefecto();
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
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM RESENA r" + condiciones)) {
                int parametro = 1;
                if (idLibro > 0) sentencia.setInt(parametro++, idLibro);
                if (idUsuario > 0) sentencia.setInt(parametro, idUsuario);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = 1;
                sentencia.setInt(parametro++, idLibro > 0 && idUsuario == 0 ? idUsuarioActual : 0);
                if (idLibro > 0) sentencia.setInt(parametro++, idLibro);
                if (idUsuario > 0) sentencia.setInt(parametro++, idUsuario);
                sentencia.setInt(parametro++, consulta.tamanioPagina());
                sentencia.setLong(parametro, (long) (consulta.pagina() - 1) * consulta.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) { while (resultado.next()) items.add(mapearResenaDTO(resultado)); }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, consulta.pagina(), consulta.tamanioPagina());
    }

    @Override
    public ResenaDTO obtenerReferencia(int id, boolean incluirImagenes) throws SQLException {
        String imagenes = incluirImagenes ? "l.portada, l.tipo_portada, u.imagen, u.tipo_imagen" : "NULL AS portada, NULL AS tipo_portada, NULL AS imagen, NULL AS tipo_imagen";
        String sql = "SELECT r.*, l.titulo, u.nombre, " + imagenes + ", FALSE AS voto_propio"
                + " FROM RESENA r JOIN LIBRO l ON l.id = r.id_libro JOIN USUARIO u ON u.id = r.id_usuario WHERE r.id = ?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next() ? mapearResenaDTO(resultado) : null; }
        }
    }

    private ResenaDTO mapearResenaDTO(ResultSet resultado) throws SQLException {
        byte[] portada = resultado.getBytes("portada");
        byte[] imagen = resultado.getBytes("imagen");
        return new ResenaDTO(resultado.getInt("id"), resultado.getInt("id_libro"), resultado.getString("titulo"),
                portada == null ? null : new ImagenDTO(portada, resultado.getString("tipo_portada")),
                resultado.getString("nombre"), imagen == null ? null : new ImagenDTO(imagen, resultado.getString("tipo_imagen")),
                resultado.getInt("puntuacion"), resultado.getString("comentario"), resultado.getObject("fecha", LocalDateTime.class),
                resultado.getInt("cantidad_votos_utiles"), resultado.getBoolean("voto_propio"), resultado.getBoolean("activo") && !resultado.getBoolean("eliminado"));
    }

    @Override
    public boolean existePublicada(int idUsuario, int idLibro) throws SQLException {
        String sql = "SELECT 1 FROM RESENA WHERE id_usuario = ? AND id_libro = ? AND activo AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idLibro);
            try (ResultSet resultado = sentencia.executeQuery()) { return resultado.next(); }
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idLibro);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return new ValoracionLibroDTO(idLibro, resultado.getLong("una"), resultado.getLong("dos"), resultado.getLong("tres"), resultado.getLong("cuatro"), resultado.getLong("cinco"));
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
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql); ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) items.add(new ValoracionLibroDTO(resultado.getInt("id_libro"), resultado.getLong("una"), resultado.getLong("dos"), resultado.getLong("tres"), resultado.getLong("cuatro"), resultado.getLong("cinco")));
        }
        return items;
    }
}
