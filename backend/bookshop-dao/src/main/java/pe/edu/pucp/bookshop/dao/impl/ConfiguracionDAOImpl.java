package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.ConfiguracionDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.Configuracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracionDAOImpl implements ConfiguracionDAO {

    @Override
    public Configuracion obtener() throws SQLException {
        String sql = """
            select costo_envio, dias_devolucion, max_solicitudes_devolucion, max_cancelaciones_mes,
                   cupon_monto_minimo, cupon_cada_pedidos, cupon_porcentaje, cupon_dias_vigencia
            from configuracion
            where id_configuracion = 1
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            rs.next();
            Configuracion configuracion = new Configuracion();
            configuracion.setCostoEnvio(rs.getDouble("costo_envio"));
            configuracion.setDiasDevolucion(rs.getInt("dias_devolucion"));
            configuracion.setMaxSolicitudesDevolucion(rs.getInt("max_solicitudes_devolucion"));
            configuracion.setMaxCancelacionesMes(rs.getInt("max_cancelaciones_mes"));
            configuracion.setCuponMontoMinimo(rs.getDouble("cupon_monto_minimo"));
            configuracion.setCuponCadaPedidos(rs.getInt("cupon_cada_pedidos"));
            configuracion.setCuponPorcentaje(rs.getInt("cupon_porcentaje"));
            configuracion.setCuponDiasVigencia(rs.getInt("cupon_dias_vigencia"));
            return configuracion;
        }
    }

    @Override
    public void modificar(Configuracion configuracion) throws SQLException {
        String sql = """
            update configuracion
            set costo_envio = ?, dias_devolucion = ?, max_solicitudes_devolucion = ?, max_cancelaciones_mes = ?,
                cupon_monto_minimo = ?, cupon_cada_pedidos = ?, cupon_porcentaje = ?, cupon_dias_vigencia = ?
            where id_configuracion = 1
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setDouble(1, configuracion.getCostoEnvio());
            pstm.setInt(2, configuracion.getDiasDevolucion());
            pstm.setInt(3, configuracion.getMaxSolicitudesDevolucion());
            pstm.setInt(4, configuracion.getMaxCancelacionesMes());
            pstm.setDouble(5, configuracion.getCuponMontoMinimo());
            pstm.setInt(6, configuracion.getCuponCadaPedidos());
            pstm.setInt(7, configuracion.getCuponPorcentaje());
            pstm.setInt(8, configuracion.getCuponDiasVigencia());
            pstm.executeUpdate();
        }
    }
}
