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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, metodo.getUsuario().getId());
            sentencia.setString(2, metodo.getTitular());
            sentencia.setString(3, metodo.getProveedor());
            sentencia.setString(4, metodo.getAlias());
            sentencia.setBoolean(5, metodo.isActivo());
            sentencia.setString(6, tipo);
            if (metodo instanceof TarjetaBancaria tarjeta) {
                sentencia.setString(7, tarjeta.getNumeroTarjetaCifrado());
                sentencia.setString(8, null);
                sentencia.setString(9, tarjeta.getTipoTarjeta().name());
            } else {
                BilleteraDigital billetera = (BilleteraDigital) metodo;
                sentencia.setString(7, null);
                sentencia.setString(8, billetera.getNumeroTelefonoCifrado());
                sentencia.setString(9, null);
            }
            sentencia.setDate(10, Date.valueOf(metodo.getFechaVencimiento()));
            sentencia.setString(11, metodo.getUsuarioCreacion());
            sentencia.setTimestamp(12, Timestamp.valueOf(metodo.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignarDatosEdicion(sentencia, metodo);
            sentencia.setBoolean(9, metodo.isActivo());
            sentencia.setString(10, metodo.getUsuarioActualizacion());
            sentencia.setTimestamp(11, Timestamp.valueOf(fecha));
            sentencia.setInt(12, metodo.getId());
            sentencia.setInt(13, metodo.getUsuario().getId());
            sentencia.setString(14, tipoMetodo(metodo));
            if (sentencia.executeUpdate() != 1) throw new SQLException("El metodo de pago no puede modificarse.");
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
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignarDatosEdicion(sentencia, metodo);
            sentencia.setString(9, metodo.getUsuarioActualizacion());
            sentencia.setTimestamp(10, Timestamp.valueOf(fecha));
            sentencia.setInt(11, metodo.getId());
            sentencia.setInt(12, metodo.getUsuario().getId());
            sentencia.setString(13, tipoMetodo(metodo));
            if (sentencia.executeUpdate() != 1) return false;
            metodo.setEliminado(false);
            metodo.setPredeterminado(false);
            metodo.setFechaActualizacion(fecha);
            return true;
        }
    }

    // Editar y recuperar guardan los mismos datos, conservando propietario y subtipo.
    private void asignarDatosEdicion(PreparedStatement sentencia, MetodoPago metodo) throws SQLException {
        sentencia.setString(1, metodo.getTitular());
        sentencia.setString(2, metodo.getProveedor());
        sentencia.setString(3, metodo.getAlias());
        if (metodo instanceof TarjetaBancaria tarjeta) {
            sentencia.setString(4, tarjeta.getNumeroTarjetaCifrado());
            sentencia.setString(5, null);
            sentencia.setString(6, tarjeta.getTipoTarjeta().name());
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            sentencia.setString(4, null);
            sentencia.setString(5, billetera.getNumeroTelefonoCifrado());
            sentencia.setString(6, null);
        }
        sentencia.setDate(7, Date.valueOf(metodo.getFechaVencimiento()));
        sentencia.setBoolean(8, metodo.isActivo());
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
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapearMetodo(resultado) : null;
            }
        }
    }

    @Override
    public MetodoPago leerParaActualizar(int id) throws SQLException {
        String sql = "SELECT * FROM METODO_PAGO WHERE id = ? FOR UPDATE";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
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
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public ResultadoPaginadoDTO<MetodoPago> listarPorUsuario(int idUsuario, int pagina, int tamanioPagina)
            throws SQLException {
        String condiciones = " FROM METODO_PAGO WHERE id_usuario = ? AND NOT eliminado";
        List<MetodoPago> metodos = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*)" + condiciones)) {
                sentencia.setInt(1, idUsuario);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT *" + condiciones
                    + " ORDER BY predeterminado DESC, fecha_creacion DESC, id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setInt(1, idUsuario);
                sentencia.setInt(2, tamanioPagina);
                sentencia.setLong(3, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) metodos.add(mapearMetodo(resultado));
                }
            }
        }
        return new ResultadoPaginadoDTO<>(metodos, total, pagina, tamanioPagina);
    }

    @Override
    public List<MetodoPago> listarParaComprobarNumeros(boolean esTarjeta) throws SQLException {
        // Incluye retirados y todos los propietarios para la unicidad global y recuperacion.
        String sql = "SELECT * FROM METODO_PAGO WHERE tipo_metodo = ? ORDER BY id FOR UPDATE";
        List<MetodoPago> metodos = new ArrayList<>();
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, esTarjeta ? "TARJETA_BANCARIA" : "BILLETERA_DIGITAL");
            try (ResultSet resultado = sentencia.executeQuery()) {
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
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setDate(2, Date.valueOf(fecha));
            sentencia.executeUpdate();
        }
    }

    @Override
    public boolean establecerPredeterminado(int idUsuario, int idMetodo, LocalDate fecha, String usuario)
            throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String bloquearPropietario = "SELECT id FROM USUARIO WHERE id = ? AND activo AND NOT eliminado FOR UPDATE";
        try (PreparedStatement sentencia = conexion.prepareStatement(bloquearPropietario)) {
            sentencia.setInt(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        String candidato = "SELECT id FROM METODO_PAGO WHERE id = ? AND id_usuario = ? "
                + "AND activo AND NOT eliminado AND fecha_vencimiento >= ? FOR UPDATE";
        try (PreparedStatement sentencia = conexion.prepareStatement(candidato)) {
            sentencia.setInt(1, idMetodo);
            sentencia.setInt(2, idUsuario);
            sentencia.setDate(3, Date.valueOf(fecha));
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return false;
            }
        }
        String retirar = "UPDATE METODO_PAGO SET predeterminado = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id_usuario = ? AND predeterminado";
        try (PreparedStatement sentencia = conexion.prepareStatement(retirar)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, idUsuario);
            sentencia.executeUpdate();
        }
        String elegir = "UPDATE METODO_PAGO SET predeterminado = TRUE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND id_usuario = ? "
                + "AND activo AND NOT eliminado AND fecha_vencimiento >= ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(elegir)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, idMetodo);
            sentencia.setInt(3, idUsuario);
            sentencia.setDate(4, Date.valueOf(fecha));
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean quitarPredeterminado(int idMetodo, String usuario) throws SQLException {
        String sql = "UPDATE METODO_PAGO SET predeterminado = FALSE, usuario_actualizacion = ?, "
                + "fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ? AND predeterminado AND NOT eliminado";
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, idMetodo);
            return sentencia.executeUpdate() == 1;
        }
    }
}
