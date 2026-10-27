package me.DAO.usuario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.DAO.catalogo.AutorDAOImpl;
import me.DAO.catalogo.CategoriaDAOImpl;
import me.DAO.catalogo.EditorialDAOImpl;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.usuario.PerfilUsuarioDTO;
import me.Model.dto.usuario.ResumenActividadUsuarioDTO;
import me.Model.dto.usuario.TarjetaUsuarioDTO;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.Usuario;

public class UsuarioDAOImpl implements UsuarioDAO {
    @Override
    public void insertar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO USUARIO (email, contrasenia, nombre, imagen, tipo_imagen, fecha_nacimiento, "
                + "activo, rol, tiempo_total, cantidad_pedidos_entregados, eliminado, usuario_creacion, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (usuario.getFechaCreacion() == null) usuario.setFechaCreacion(LocalDateTime.now());
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, usuario.getEmail());
            statement.setString(2, usuario.getContrasenia());
            statement.setString(3, usuario.getNombre());
            statement.setBytes(4, usuario.getImagen());
            statement.setString(5, usuario.getTipoImagen());
            statement.setTimestamp(6, Timestamp.valueOf(usuario.getFechaNacimiento()));
            statement.setBoolean(7, usuario.isActivo());
            statement.setString(8, usuario.getRol().name());
            statement.setLong(9, usuario.getTiempoTotal().getSeconds());
            statement.setInt(10, usuario.getCantidadPedidosEntregados());
            statement.setBoolean(11, usuario.isEliminado());
            statement.setString(12, usuario.getUsuarioCreacion());
            statement.setTimestamp(13, Timestamp.valueOf(usuario.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se genero el identificador del usuario.");
                usuario.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Usuario usuario) throws SQLException {
        // Mi perfil edita nombre y nacimiento. Correo, rol, clave, foto y contadores tienen otro tratamiento.
        String sql = "UPDATE USUARIO SET nombre = ?, fecha_nacimiento = ?, usuario_actualizacion = ?, "
                + "fecha_actualizacion = ? WHERE id = ? AND NOT eliminado";
        LocalDateTime fecha = LocalDateTime.now();
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario.getNombre());
            statement.setTimestamp(2, Timestamp.valueOf(usuario.getFechaNacimiento()));
            statement.setString(3, usuario.getUsuarioActualizacion());
            statement.setTimestamp(4, Timestamp.valueOf(fecha));
            statement.setInt(5, usuario.getId());
            if (statement.executeUpdate() != 1) throw new SQLException("El usuario no puede modificarse.");
            usuario.setFechaActualizacion(fecha);
        }
    }

    @Override
    public Usuario leer(int id) throws SQLException {
        String sql = "SELECT * FROM USUARIO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearUsuario(resultado) : null;
            }
        }
    }

    @Override
    public Usuario leerParaActualizar(int id) throws SQLException {
        String sql = "SELECT * FROM USUARIO WHERE id = ? FOR UPDATE";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearUsuario(resultado) : null;
            }
        }
    }

    private Usuario mapearUsuario(ResultSet resultado) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(resultado.getInt("id"));
        usuario.setEmail(resultado.getString("email"));
        usuario.setContrasenia(resultado.getString("contrasenia"));
        usuario.setNombre(resultado.getString("nombre"));
        usuario.setImagen(resultado.getBytes("imagen"));
        usuario.setTipoImagen(resultado.getString("tipo_imagen"));
        usuario.setFechaNacimiento(resultado.getObject("fecha_nacimiento", LocalDateTime.class));
        usuario.setActivo(resultado.getBoolean("activo"));
        usuario.setRol(Rol.valueOf(resultado.getString("rol")));
        usuario.setTiempoTotal(Duration.ofSeconds(resultado.getLong("tiempo_total")));
        usuario.setCantidadPedidosEntregados(resultado.getInt("cantidad_pedidos_entregados"));
        usuario.setEliminado(resultado.getBoolean("eliminado"));
        usuario.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        usuario.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        usuario.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        usuario.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return usuario;
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE USUARIO SET eliminado = TRUE, activo = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public Usuario obtenerPorEmail(String email) throws SQLException {
        // Se incluyen desactivados para permitir reactivacion tras verificar la contrasenia en Business.
        String sql = "SELECT * FROM USUARIO WHERE email = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearUsuario(resultado) : null;
            }
        }
    }

    @Override
    public PerfilUsuarioDTO obtenerPerfil(int id) throws SQLException {
        String sql = "SELECT id, nombre, email, imagen, tipo_imagen, fecha_nacimiento "
                + "FROM USUARIO WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return null;
                byte[] contenido = resultado.getBytes("imagen");
                ImagenDTO imagen = contenido == null ? null
                        : new ImagenDTO(contenido, resultado.getString("tipo_imagen"));
                TarjetaUsuarioDTO tarjeta = new TarjetaUsuarioDTO(resultado.getInt("id"),
                        resultado.getString("nombre"), resultado.getString("email"), imagen);
                return new PerfilUsuarioDTO(tarjeta,
                        resultado.getObject("fecha_nacimiento", LocalDateTime.class).toLocalDate());
            }
        }
    }

    @Override
    public boolean cambiarImagen(int id, ImagenDTO imagen, String usuario) throws SQLException {
        String sql = "UPDATE USUARIO SET imagen = ?, tipo_imagen = ?, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND activo AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setBytes(1, imagen == null ? null : imagen.contenido());
            statement.setString(2, imagen == null ? null : imagen.tipoMime());
            statement.setString(3, usuario);
            statement.setInt(4, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarActivo(int id, boolean activo, String usuario) throws SQLException {
        String sql = "UPDATE USUARIO SET activo = ?, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado AND activo <> ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setBoolean(1, activo);
            statement.setString(2, usuario);
            statement.setInt(3, id);
            statement.setBoolean(4, activo);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarContrasenia(int id, String hash, String usuario) throws SQLException {
        String sql = "UPDATE USUARIO SET contrasenia = ?, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND activo AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, hash);
            statement.setString(2, usuario);
            statement.setInt(3, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void actualizarTiempoTotal(int id, Duration tiempo, String usuario) throws SQLException {
        if (tiempo == null || tiempo.isNegative()) throw new IllegalArgumentException("El tiempo no puede ser negativo.");
        if (tiempo.isZero()) return;
        String sql = "UPDATE USUARIO SET tiempo_total = tiempo_total + ?, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setLong(1, tiempo.getSeconds());
            statement.setString(2, usuario);
            statement.setInt(3, id);
            if (statement.executeUpdate() != 1) throw new SQLException("El usuario no puede acumular tiempo.");
        }
    }

    @Override
    public boolean incrementarPedidosEntregados(int id, String usuario) throws SQLException {
        // Business llama solo despues de cambiar por primera vez el pedido a ENTREGADO.
        String sql = "UPDATE USUARIO SET cantidad_pedidos_entregados = cantidad_pedidos_entregados + 1, "
                + "usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND NOT eliminado AND cantidad_pedidos_entregados < 2147483647";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<ResumenActividadUsuarioDTO> listarActividadUsuarios(LocalDate inicio, LocalDate fin, Integer maximo)
            throws SQLException {
        if (inicio == null || fin == null || fin.isBefore(inicio) || (maximo != null && maximo < 1)) {
            throw new IllegalArgumentException("Periodo o cantidad maxima invalidos.");
        }
        String sql = "SELECT u.id, u.nombre, u.email, u.activo, u.tiempo_total, "
                + "COALESCE(g.total_gastado, 0) AS total_gastado FROM USUARIO u LEFT JOIN "
                + "(SELECT id_usuario, SUM(total) AS total_gastado FROM PEDIDO "
                + "WHERE NOT eliminado AND estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO') "
                + "AND fecha_pago >= ? AND fecha_pago < ? GROUP BY id_usuario) g ON g.id_usuario = u.id "
                + "WHERE NOT u.eliminado ORDER BY total_gastado DESC, u.nombre, u.id"
                + (maximo == null ? "" : " LIMIT ?");
        List<ResumenActividadUsuarioDTO> actividad = new ArrayList<>();
        List<Integer> idsUsuarios = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
            if (maximo != null) statement.setInt(3, maximo);
            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    int id = resultado.getInt("id");
                    idsUsuarios.add(id);
                    TarjetaUsuarioDTO tarjeta = new TarjetaUsuarioDTO(id, resultado.getString("nombre"),
                            resultado.getString("email"), null);
                    BigDecimal gastado = resultado.getBigDecimal("total_gastado");
                    actividad.add(new ResumenActividadUsuarioDTO(tarjeta, resultado.getBoolean("activo"), gastado,
                            Duration.ofSeconds(resultado.getLong("tiempo_total")), null, null, null));
                }
            }
        }
        if (actividad.isEmpty()) return actividad;
        // Tres consultas para el conjunto completo; no reconstruyen referencias ni consultan por cada fila.
        Map<Integer, TarjetaReferenciaDTO> categorias = new CategoriaDAOImpl().obtenerMasCompradas(idsUsuarios, inicio, fin);
        Map<Integer, TarjetaReferenciaDTO> autores = new AutorDAOImpl().obtenerMasCompradas(idsUsuarios, inicio, fin);
        Map<Integer, TarjetaReferenciaDTO> editoriales = new EditorialDAOImpl().obtenerMasCompradas(idsUsuarios, inicio, fin);
        List<ResumenActividadUsuarioDTO> reporte = new ArrayList<>();
        for (ResumenActividadUsuarioDTO usuario : actividad) {
            int id = usuario.usuario().id();
            reporte.add(new ResumenActividadUsuarioDTO(usuario.usuario(), usuario.activo(), usuario.totalGastado(),
                    usuario.tiempoTotal(), categorias.get(id), autores.get(id), editoriales.get(id)));
        }
        return reporte;
    }
}
