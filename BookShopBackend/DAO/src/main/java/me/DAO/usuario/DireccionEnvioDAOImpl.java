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
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.entities.compra.DireccionEnvio;
import me.Model.entities.usuario.Usuario;

public class DireccionEnvioDAOImpl implements DireccionEnvioDAO {
    @Override
    public void insertar(DireccionEnvio direccion) throws SQLException {
        String sql = "INSERT INTO DIRECCION_ENVIO (id_usuario, alias, calle, distrito, ciudad, "
                + "referencia, activo, predeterminada, eliminado, usuario_creacion, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?, ?)";
        if (direccion.getFechaCreacion() == null) direccion.setFechaCreacion(LocalDateTime.now());
        // El alta y la eleccion opcional de predeterminada se confirman juntas en Business.
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, direccion.getUsuario().getId());
            statement.setString(2, direccion.getAlias());
            statement.setString(3, direccion.getCalle());
            statement.setString(4, direccion.getDistrito());
            statement.setString(5, direccion.getCiudad());
            statement.setString(6, direccion.getReferencia());
            statement.setBoolean(7, direccion.isActivo());
            statement.setBoolean(8, direccion.isEliminado());
            statement.setString(9, direccion.getUsuarioCreacion());
            statement.setTimestamp(10, Timestamp.valueOf(direccion.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se genero el identificador de la direccion.");
                direccion.setId(claves.getInt(1));
            }
            // El cambio de predeterminado comprueba el candidato y bloquea al propietario.
            direccion.setPredeterminada(false);
        }
    }

    @Override
    public void modificar(DireccionEnvio direccion) throws SQLException {
        String sql = "UPDATE DIRECCION_ENVIO SET alias = ?, calle = ?, distrito = ?, ciudad = ?, "
                + "referencia = ?, activo = ?, predeterminada = CASE WHEN ? THEN predeterminada ELSE FALSE END, "
                + "usuario_actualizacion = ?, fecha_actualizacion = ? WHERE id = ? AND NOT eliminado";
        LocalDateTime fecha = LocalDateTime.now();
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, direccion.getAlias());
            statement.setString(2, direccion.getCalle());
            statement.setString(3, direccion.getDistrito());
            statement.setString(4, direccion.getCiudad());
            statement.setString(5, direccion.getReferencia());
            statement.setBoolean(6, direccion.isActivo());
            statement.setBoolean(7, direccion.isActivo());
            statement.setString(8, direccion.getUsuarioActualizacion());
            statement.setTimestamp(9, Timestamp.valueOf(fecha));
            statement.setInt(10, direccion.getId());
            if (statement.executeUpdate() != 1) throw new SQLException("La direccion no puede modificarse.");
            direccion.setFechaActualizacion(fecha);
        }
    }

    @Override
    public DireccionEnvio leer(int id) throws SQLException {
        String sql = "SELECT * FROM DIRECCION_ENVIO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearDireccion(resultado) : null;
            }
        }
    }

    @Override
    public DireccionEnvio leerParaActualizar(int id) throws SQLException {
        String sql = "SELECT * FROM DIRECCION_ENVIO WHERE id = ? FOR UPDATE";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearDireccion(resultado) : null;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE DIRECCION_ENVIO SET eliminado = TRUE, activo = FALSE, predeterminada = FALSE, "
                + "usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    private DireccionEnvio mapearDireccion(ResultSet resultado) throws SQLException {
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setId(resultado.getInt("id"));
        Usuario propietario = new Usuario();
        propietario.setId(resultado.getInt("id_usuario"));
        direccion.setUsuario(propietario);
        direccion.setAlias(resultado.getString("alias"));
        direccion.setCalle(resultado.getString("calle"));
        direccion.setDistrito(resultado.getString("distrito"));
        direccion.setCiudad(resultado.getString("ciudad"));
        direccion.setReferencia(resultado.getString("referencia"));
        direccion.setActivo(resultado.getBoolean("activo"));
        direccion.setEliminado(resultado.getBoolean("eliminado"));
        direccion.setPredeterminada(resultado.getBoolean("predeterminada"));
        direccion.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        direccion.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        direccion.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        direccion.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return direccion;
    }

    @Override
    public ResultadoPaginadoDTO<DireccionEnvioDTO> listarPorUsuario(int idUsuario, int pagina, int tamano)
            throws SQLException {
        if (pagina < 1 || tamano < 1) throw new IllegalArgumentException("Pagina y tamano deben ser positivos.");
        String condiciones = " FROM DIRECCION_ENVIO WHERE id_usuario = ? AND activo AND NOT eliminado";
        List<DireccionEnvioDTO> direcciones = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*)" + condiciones)) {
                statement.setInt(1, idUsuario);
                try (ResultSet resultado = statement.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT id, alias, calle, distrito, ciudad, referencia, predeterminada" + condiciones
                    + " ORDER BY predeterminada DESC, fecha_creacion DESC, id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                statement.setInt(1, idUsuario);
                statement.setInt(2, tamano);
                statement.setLong(3, (long) (pagina - 1) * tamano);
                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        direcciones.add(new DireccionEnvioDTO(resultado.getInt("id"), resultado.getString("alias"),
                                resultado.getString("calle"), resultado.getString("distrito"),
                                resultado.getString("ciudad"), resultado.getString("referencia"),
                                resultado.getBoolean("predeterminada")));
                    }
                }
            }
        }
        return new ResultadoPaginadoDTO<>(direcciones, total, pagina, tamano);
    }

    @Override
    public boolean establecerPredeterminado(int idUsuario, int idDireccion, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String bloquearPropietario = "SELECT id FROM USUARIO WHERE id = ? AND activo AND NOT eliminado FOR UPDATE";
        try (PreparedStatement statement = conexion.prepareStatement(bloquearPropietario)) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        String candidato = "SELECT id FROM DIRECCION_ENVIO WHERE id = ? AND id_usuario = ? "
                + "AND activo AND NOT eliminado FOR UPDATE";
        try (PreparedStatement statement = conexion.prepareStatement(candidato)) {
            statement.setInt(1, idDireccion);
            statement.setInt(2, idUsuario);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        // La verificacion se completa antes de retirar la direccion anterior.
        String retirar = "UPDATE DIRECCION_ENVIO SET predeterminada = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_usuario = ? AND predeterminada";
        try (PreparedStatement statement = conexion.prepareStatement(retirar)) {
            statement.setString(1, usuario);
            statement.setInt(2, idUsuario);
            statement.executeUpdate();
        }
        String elegir = "UPDATE DIRECCION_ENVIO SET predeterminada = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND id_usuario = ? AND activo AND NOT eliminado";
        try (PreparedStatement statement = conexion.prepareStatement(elegir)) {
            statement.setString(1, usuario);
            statement.setInt(2, idDireccion);
            statement.setInt(3, idUsuario);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean quitarPredeterminado(int idDireccion, String usuario) throws SQLException {
        String sql = "UPDATE DIRECCION_ENVIO SET predeterminada = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND predeterminada AND NOT eliminado";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, idDireccion);
            return statement.executeUpdate() == 1;
        }
    }
}
