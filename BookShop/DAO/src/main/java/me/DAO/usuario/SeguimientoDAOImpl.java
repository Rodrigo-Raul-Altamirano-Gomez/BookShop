package me.DAO.usuario;

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
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.OrdenReferencia;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.TipoSeguimiento;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.usuario.Seguimiento;
import me.Model.entities.usuario.SeguimientoAutor;
import me.Model.entities.usuario.SeguimientoCategoria;
import me.Model.entities.usuario.SeguimientoEditorial;
import me.Model.entities.usuario.Usuario;

public class SeguimientoDAOImpl implements SeguimientoDAO {
    @Override
    public void insertar(Seguimiento seguimiento) throws SQLException {
        TipoSeguimiento tipo;
        int idReferencia;
        if (seguimiento instanceof SeguimientoAutor autor) {
            tipo = TipoSeguimiento.AUTOR;
            idReferencia = autor.getAutor().getId();
        } else if (seguimiento instanceof SeguimientoCategoria categoria) {
            tipo = TipoSeguimiento.CATEGORIA;
            idReferencia = categoria.getCategoria().getId();
        } else if (seguimiento instanceof SeguimientoEditorial editorial) {
            tipo = TipoSeguimiento.EDITORIAL;
            idReferencia = editorial.getEditorial().getId();
        } else {
            throw new SQLException("El tipo de seguimiento no es valido.");
        }
        String sql = "INSERT INTO SEGUIMIENTO (tipo_seguimiento, id_usuario, " + columnaReferencia(tipo)
                + ", eliminado, usuario_creacion, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?)";
        if (seguimiento.getFechaCreacion() == null) seguimiento.setFechaCreacion(LocalDateTime.now());
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tipo.name());
            statement.setInt(2, seguimiento.getUsuario().getId());
            statement.setInt(3, idReferencia);
            statement.setBoolean(4, seguimiento.isEliminado());
            statement.setString(5, seguimiento.getUsuarioCreacion());
            statement.setTimestamp(6, Timestamp.valueOf(seguimiento.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se genero el identificador del seguimiento.");
                seguimiento.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public Seguimiento leer(int id) throws SQLException {
        String sql = "SELECT * FROM SEGUIMIENTO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return null;
                Seguimiento seguimiento;
                switch (TipoSeguimiento.valueOf(resultado.getString("tipo_seguimiento"))) {
                    case AUTOR -> {
                        Autor autor = new Autor();
                        autor.setId(resultado.getInt("id_autor"));
                        SeguimientoAutor seguido = new SeguimientoAutor();
                        seguido.setAutor(autor);
                        seguimiento = seguido;
                    }
                    case CATEGORIA -> {
                        Categoria categoria = new Categoria();
                        categoria.setId(resultado.getInt("id_categoria"));
                        SeguimientoCategoria seguido = new SeguimientoCategoria();
                        seguido.setCategoria(categoria);
                        seguimiento = seguido;
                    }
                    case EDITORIAL -> {
                        Editorial editorial = new Editorial();
                        editorial.setId(resultado.getInt("id_editorial"));
                        SeguimientoEditorial seguido = new SeguimientoEditorial();
                        seguido.setEditorial(editorial);
                        seguimiento = seguido;
                    }
                    default -> throw new SQLException("El tipo de seguimiento almacenado no es valido.");
                }
                seguimiento.setId(resultado.getInt("id"));
                Usuario propietario = new Usuario();
                propietario.setId(resultado.getInt("id_usuario"));
                seguimiento.setUsuario(propietario);
                seguimiento.setEliminado(resultado.getBoolean("eliminado"));
                seguimiento.setUsuarioCreacion(resultado.getString("usuario_creacion"));
                seguimiento.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
                seguimiento.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
                seguimiento.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
                return seguimiento;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE SEGUIMIENTO SET eliminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void guardar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws SQLException {
        String sql = "INSERT INTO SEGUIMIENTO (tipo_seguimiento, id_usuario, " + columnaReferencia(tipo)
                + ", usuario_creacion) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "usuario_actualizacion = CASE WHEN eliminado THEN ? ELSE usuario_actualizacion END, "
                + "fecha_actualizacion = CASE WHEN eliminado THEN CURRENT_TIMESTAMP ELSE fecha_actualizacion END, "
                + "eliminado = FALSE";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, tipo.name());
            statement.setInt(2, idUsuario);
            statement.setInt(3, idReferencia);
            statement.setString(4, usuario);
            statement.setString(5, usuario);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean retirar(int idUsuario, TipoSeguimiento tipo, int idReferencia, String usuario) throws SQLException {
        String sql = "UPDATE SEGUIMIENTO SET eliminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE tipo_seguimiento = ? "
                + "AND id_usuario = ? AND " + columnaReferencia(tipo) + " = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setString(2, tipo.name());
            statement.setInt(3, idUsuario);
            statement.setInt(4, idReferencia);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaReferenciaDTO> listarPorUsuario(int idUsuario, TipoSeguimiento tipo,
                                                                     OrdenReferencia orden, int pagina, int tamano)
            throws SQLException {
        if (pagina < 1 || tamano < 1) throw new IllegalArgumentException("Pagina y tamano deben ser positivos.");
        String condiciones = " FROM SEGUIMIENTO s JOIN " + tipo.name() + " r ON r.id = s."
                + columnaReferencia(tipo) + " WHERE s.tipo_seguimiento = ? AND s.id_usuario = ? "
                + "AND NOT s.eliminado AND r.activo AND NOT r.eliminado";
        String ordenSQL = orden == OrdenReferencia.MAS_RECIENTES
                ? "COALESCE(s.fecha_actualizacion, s.fecha_creacion) DESC, s.id DESC" : "r.nombre, r.id";
        List<TarjetaReferenciaDTO> tarjetas = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*)" + condiciones)) {
                statement.setString(1, tipo.name());
                statement.setInt(2, idUsuario);
                try (ResultSet resultado = statement.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT r.id, r.nombre, r.imagen, r.tipo_imagen" + condiciones
                    + " ORDER BY " + ordenSQL + " LIMIT ? OFFSET ?";
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                statement.setString(1, tipo.name());
                statement.setInt(2, idUsuario);
                statement.setInt(3, tamano);
                statement.setLong(4, (long) (pagina - 1) * tamano);
                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        byte[] contenido = resultado.getBytes("imagen");
                        ImagenDTO imagen = contenido == null ? null
                                : new ImagenDTO(contenido, resultado.getString("tipo_imagen"));
                        tarjetas.add(new TarjetaReferenciaDTO(resultado.getInt("id"),
                                resultado.getString("nombre"), imagen));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(tarjetas, total, pagina, tamano);
    }

    private String columnaReferencia(TipoSeguimiento tipo) {
        return switch (tipo) {
            case AUTOR -> "id_autor";
            case CATEGORIA -> "id_categoria";
            case EDITORIAL -> "id_editorial";
        };
    }
}
