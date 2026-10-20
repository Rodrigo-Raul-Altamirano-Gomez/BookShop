package me.DAO.usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.entities.compra.BilleteraDigital;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.compra.TipoTarjeta;
import me.Model.entities.usuario.Usuario;

public class MetodoPagoDAOImpl implements MetodoPagoDAO {
    @Override
    public void insertar(MetodoPago metodo) throws SQLException {
        String tipo = tipoMetodo(metodo);
        String sql = "INSERT INTO METODO_PAGO (id_usuario, titular, proveedor, alias, activo, "
                + "tipo_metodo, numero_tarjeta_cifrado, numero_telefono_cifrado, tipo_tarjeta, "
                + "fecha_vencimiento, predeterminado, usuario_creacion, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)";
        if (metodo.getFechaCreacion() == null) metodo.setFechaCreacion(LocalDateTime.now());
        // Comparar numeros, insertar y elegir predeterminado deben poder compartir el contexto.
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, metodo.getUsuario().getId());
            statement.setString(2, metodo.getTitular());
            statement.setString(3, metodo.getProveedor());
            statement.setString(4, metodo.getAlias());
            statement.setBoolean(5, metodo.isActivo());
            statement.setString(6, tipo);
            if (metodo instanceof TarjetaBancaria tarjeta) {
                statement.setString(7, tarjeta.getNumeroTarjetaCifrado());
                statement.setString(8, null);
                statement.setString(9, tarjeta.getTipoTarjeta().name());
            } else {
                BilleteraDigital billetera = (BilleteraDigital) metodo;
                statement.setString(7, null);
                statement.setString(8, billetera.getNumeroTelefonoCifrado());
                statement.setString(9, null);
            }
            statement.setDate(10, Date.valueOf(metodo.getFechaVencimiento()));
            statement.setString(11, metodo.getUsuarioCreacion());
            statement.setTimestamp(12, Timestamp.valueOf(metodo.getFechaCreacion()));
            statement.executeUpdate();
            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se genero el identificador del metodo de pago.");
                metodo.setId(claves.getInt(1));
            }
            metodo.setPredeterminado(false);
        }
    }

    @Override
    public void modificar(MetodoPago metodo) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET titular = ?, proveedor = ?, alias = ?, "
                + "numero_tarjeta_cifrado = ?, numero_telefono_cifrado = ?, tipo_tarjeta = ?, "
                + "fecha_vencimiento = ?, activo = ?, "
                + "predeterminado = CASE WHEN ? THEN predeterminado ELSE FALSE END, "
                + "usuario_actualizacion = ?, fecha_actualizacion = ? "
                + "WHERE id = ? AND id_usuario = ? AND tipo_metodo = ? AND NOT eliminado";
        LocalDateTime fecha = LocalDateTime.now();
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            asignarDatosEdicion(statement, metodo);
            statement.setBoolean(9, metodo.isActivo());
            statement.setString(10, metodo.getUsuarioActualizacion());
            statement.setTimestamp(11, Timestamp.valueOf(fecha));
            statement.setInt(12, metodo.getId());
            statement.setInt(13, metodo.getUsuario().getId());
            statement.setString(14, tipoMetodo(metodo));
            if (statement.executeUpdate() != 1) throw new SQLException("El metodo de pago no puede modificarse.");
            metodo.setFechaActualizacion(fecha);
        }
    }

    @Override
    public boolean recuperar(MetodoPago metodo) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET titular = ?, proveedor = ?, alias = ?, "
                + "numero_tarjeta_cifrado = ?, numero_telefono_cifrado = ?, tipo_tarjeta = ?, "
                + "fecha_vencimiento = ?, activo = ?, eliminado = FALSE, predeterminado = FALSE, "
                + "usuario_actualizacion = ?, fecha_actualizacion = ? "
                + "WHERE id = ? AND id_usuario = ? AND tipo_metodo = ? AND eliminado";
        LocalDateTime fecha = LocalDateTime.now();
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            asignarDatosEdicion(statement, metodo);
            statement.setString(9, metodo.getUsuarioActualizacion());
            statement.setTimestamp(10, Timestamp.valueOf(fecha));
            statement.setInt(11, metodo.getId());
            statement.setInt(12, metodo.getUsuario().getId());
            statement.setString(13, tipoMetodo(metodo));
            if (statement.executeUpdate() != 1) return false;
            metodo.setEliminado(false);
            metodo.setPredeterminado(false);
            metodo.setFechaActualizacion(fecha);
            return true;
        }
    }

    // Editar y recuperar guardan los mismos datos, conservando propietario y subtipo.
    private void asignarDatosEdicion(PreparedStatement statement, MetodoPago metodo) throws SQLException {
        tipoMetodo(metodo);
        statement.setString(1, metodo.getTitular());
        statement.setString(2, metodo.getProveedor());
        statement.setString(3, metodo.getAlias());
        if (metodo instanceof TarjetaBancaria tarjeta) {
            statement.setString(4, tarjeta.getNumeroTarjetaCifrado());
            statement.setString(5, null);
            statement.setString(6, tarjeta.getTipoTarjeta().name());
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            statement.setString(4, null);
            statement.setString(5, billetera.getNumeroTelefonoCifrado());
            statement.setString(6, null);
        }
        statement.setDate(7, Date.valueOf(metodo.getFechaVencimiento()));
        statement.setBoolean(8, metodo.isActivo());
    }

    private String tipoMetodo(MetodoPago metodo) throws SQLException {
        if (metodo instanceof TarjetaBancaria) return "TARJETA_BANCARIA";
        if (metodo instanceof BilleteraDigital) return "BILLETERA_DIGITAL";
        throw new SQLException("El subtipo de metodo de pago no es valido.");
    }

    @Override
    public MetodoPago leer(int id) throws SQLException {
        String sql = "SELECT * FROM METODO_PAGO WHERE id = ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearMetodo(resultado) : null;
            }
        }
    }

    @Override
    public MetodoPago leerParaActualizar(int id) throws SQLException {
        String sql = "SELECT * FROM METODO_PAGO WHERE id = ? FOR UPDATE";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearMetodo(resultado) : null;
            }
        }
    }

    private MetodoPago mapearMetodo(ResultSet resultado) throws SQLException {
        MetodoPago metodo;
        String tipo = resultado.getString("tipo_metodo");
        if ("TARJETA_BANCARIA".equals(tipo)) {
            TarjetaBancaria tarjeta = new TarjetaBancaria();
            tarjeta.setNumeroTarjetaCifrado(resultado.getString("numero_tarjeta_cifrado"));
            tarjeta.setTipoTarjeta(TipoTarjeta.valueOf(resultado.getString("tipo_tarjeta")));
            metodo = tarjeta;
        } else if ("BILLETERA_DIGITAL".equals(tipo)) {
            BilleteraDigital billetera = new BilleteraDigital();
            billetera.setNumeroTelefonoCifrado(resultado.getString("numero_telefono_cifrado"));
            metodo = billetera;
        } else {
            throw new SQLException("El subtipo de metodo de pago almacenado no es valido.");
        }
        metodo.setId(resultado.getInt("id"));
        Usuario propietario = new Usuario();
        propietario.setId(resultado.getInt("id_usuario"));
        metodo.setUsuario(propietario);
        metodo.setTitular(resultado.getString("titular"));
        metodo.setProveedor(resultado.getString("proveedor"));
        metodo.setAlias(resultado.getString("alias"));
        metodo.setFechaVencimiento(resultado.getObject("fecha_vencimiento", LocalDate.class));
        metodo.setActivo(resultado.getBoolean("activo"));
        metodo.setEliminado(resultado.getBoolean("eliminado"));
        metodo.setPredeterminado(resultado.getBoolean("predeterminado"));
        metodo.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        metodo.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        metodo.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        metodo.setFechaActualizacion(resultado.getObject("fecha_actualizacion", LocalDateTime.class));
        return metodo;
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET eliminado = TRUE, predeterminado = FALSE, "
                + "usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND NOT eliminado";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<MetodoPago> listarPorUsuario(int idUsuario, int pagina, int tamano)
            throws SQLException {
        if (pagina < 1 || tamano < 1) throw new IllegalArgumentException("Pagina y tamano deben ser positivos.");
        String condiciones = " FROM METODO_PAGO WHERE id_usuario = ? AND NOT eliminado";
        List<MetodoPago> metodos = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement statement = conexion.prepareStatement("SELECT COUNT(*)" + condiciones)) {
                statement.setInt(1, idUsuario);
                try (ResultSet resultado = statement.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT *" + condiciones
                    + " ORDER BY predeterminado DESC, fecha_creacion DESC, id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                statement.setInt(1, idUsuario);
                statement.setInt(2, tamano);
                statement.setLong(3, (long) (pagina - 1) * tamano);
                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) metodos.add(mapearMetodo(resultado));
                }
            }
        }
        return new ResultadoPaginadoDTO<>(metodos, total, pagina, tamano);
    }

    @Override
    public List<MetodoPago> listarParaComprobarNumeros(boolean esTarjeta) throws SQLException {
        // Incluye retirados y todos los propietarios para la unicidad global y recuperacion.
        String sql = "SELECT * FROM METODO_PAGO WHERE tipo_metodo = ? ORDER BY id FOR UPDATE";
        List<MetodoPago> metodos = new ArrayList<>();
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, esTarjeta ? "TARJETA_BANCARIA" : "BILLETERA_DIGITAL");
            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) metodos.add(mapearMetodo(resultado));
            }
        }
        return metodos;
    }

    @Override
    public void desactivarVencidos(LocalDate fecha, String usuario) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET activo = FALSE, predeterminado = FALSE, "
                + "usuario_actualizacion = ?, fecha_actualizacion = CURRENT_TIMESTAMP "
                + "WHERE activo AND NOT eliminado AND fecha_vencimiento < ?";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setDate(2, Date.valueOf(fecha));
            statement.executeUpdate();
        }
    }

    @Override
    public boolean establecerPredeterminado(int idUsuario, int idMetodo, LocalDate fecha, String usuario)
            throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String bloquearPropietario = "SELECT id FROM USUARIO WHERE id = ? AND activo AND NOT eliminado FOR UPDATE";
        try (PreparedStatement statement = conexion.prepareStatement(bloquearPropietario)) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        String candidato = "SELECT id FROM METODO_PAGO WHERE id = ? AND id_usuario = ? "
                + "AND activo AND NOT eliminado AND fecha_vencimiento >= ? FOR UPDATE";
        try (PreparedStatement statement = conexion.prepareStatement(candidato)) {
            statement.setInt(1, idMetodo);
            statement.setInt(2, idUsuario);
            statement.setDate(3, Date.valueOf(fecha));
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        String retirar = "UPDATE METODO_PAGO SET predeterminado = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_usuario = ? AND predeterminado";
        try (PreparedStatement statement = conexion.prepareStatement(retirar)) {
            statement.setString(1, usuario);
            statement.setInt(2, idUsuario);
            statement.executeUpdate();
        }
        String elegir = "UPDATE METODO_PAGO SET predeterminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND id_usuario = ? "
                + "AND activo AND NOT eliminado AND fecha_vencimiento >= ?";
        try (PreparedStatement statement = conexion.prepareStatement(elegir)) {
            statement.setString(1, usuario);
            statement.setInt(2, idMetodo);
            statement.setInt(3, idUsuario);
            statement.setDate(4, Date.valueOf(fecha));
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean quitarPredeterminado(int idMetodo, String usuario) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET predeterminado = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND predeterminado AND NOT eliminado";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, idMetodo);
            return statement.executeUpdate() == 1;
        }
    }
}
