package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.PedidoDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.Cupon;
import pe.edu.pucp.bookshop.model.DetallePedido;
import pe.edu.pucp.bookshop.model.Direccion;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.EstadoPedido;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.MetodoPago;
import pe.edu.pucp.bookshop.model.Pedido;
import pe.edu.pucp.bookshop.model.TipoMetodoPago;
import pe.edu.pucp.bookshop.model.Usuario;
import pe.edu.pucp.bookshop.model.dto.Venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAOImpl implements PedidoDAO {

    // El pedido con su cliente, su dirección, su método de pago y su cupón (sin los detalles)
    @Override
    public Pedido obtener(int id) throws SQLException {
        String sql = """
            select p.id_pedido, p.estado, p.fecha_compra, p.fecha_salida, p.fecha_entrega, p.fecha_cancelacion, p.fecha_devolucion,
                   p.codigo_transaccion, p.codigo_seguimiento, p.subtotal, p.descuento_cupon, p.costo_envio, p.total,
                   p.contado_para_cupon,
                   u.id_usuario, u.nombre_completo, u.correo,
                   d.id_direccion, d.alias, d.direccion, d.distrito, d.ciudad,
                   m.id_metodo_pago, m.tipo, m.numero, m.proveedor,
                   c.id_cupon, c.codigo, c.porcentaje
            from pedido p
            join usuario u on u.id_usuario = p.fid_usuario
            join direccion d on d.id_direccion = p.fid_direccion
            join metodo_pago m on m.id_metodo_pago = p.fid_metodo_pago
            left join cupon c on c.id_cupon = p.fid_cupon
            where p.id_pedido = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Pedido pedido = new Pedido();
                pedido.setId(rs.getInt("id_pedido"));
                pedido.setEstado(EstadoPedido.valueOf(rs.getString("estado")));
                pedido.setFechaCompra(rs.getObject("fecha_compra", LocalDateTime.class));
                pedido.setFechaSalida(rs.getObject("fecha_salida", LocalDateTime.class));
                pedido.setFechaEntrega(rs.getObject("fecha_entrega", LocalDateTime.class));
                pedido.setFechaCancelacion(rs.getObject("fecha_cancelacion", LocalDateTime.class));
                pedido.setFechaDevolucion(rs.getObject("fecha_devolucion", LocalDateTime.class));
                pedido.setCodigoTransaccion(rs.getString("codigo_transaccion"));
                pedido.setCodigoSeguimiento(rs.getString("codigo_seguimiento"));
                pedido.setSubtotal(rs.getDouble("subtotal"));
                pedido.setDescuentoCupon(rs.getDouble("descuento_cupon"));
                pedido.setCostoEnvio(rs.getDouble("costo_envio"));
                pedido.setTotal(rs.getDouble("total"));
                pedido.setContadoParaCupon(rs.getBoolean("contado_para_cupon"));

                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id_usuario"));
                usuario.setNombreCompleto(rs.getString("nombre_completo"));
                usuario.setCorreo(rs.getString("correo"));
                pedido.setUsuario(usuario);

                Direccion direccion = new Direccion();
                direccion.setId(rs.getInt("id_direccion"));
                direccion.setAlias(rs.getString("alias"));
                direccion.setDireccion(rs.getString("direccion"));
                direccion.setDistrito(rs.getString("distrito"));
                direccion.setCiudad(rs.getString("ciudad"));
                pedido.setDireccion(direccion);

                MetodoPago metodo = new MetodoPago();
                metodo.setId(rs.getInt("id_metodo_pago"));
                metodo.setTipo(TipoMetodoPago.valueOf(rs.getString("tipo")));
                metodo.setNumero(rs.getString("numero"));
                metodo.setProveedor(rs.getString("proveedor"));
                pedido.setMetodoPago(metodo);

                int idCupon = rs.getInt("id_cupon");
                if (!rs.wasNull()) {
                    Cupon cupon = new Cupon();
                    cupon.setId(idCupon);
                    cupon.setCodigo(rs.getString("codigo"));
                    cupon.setPorcentaje(rs.getInt("porcentaje"));
                    pedido.setCupon(cupon);
                }
                return pedido;
            }
        }
    }

    @Override
    public List<DetallePedido> listarDetalles(int idPedido) throws SQLException {
        String sql = """
            select d.id_detalle_pedido, d.cantidad, d.precio_unitario, d.descuento_unitario, d.subtotal,
                   l.id_libro, l.titulo, l.portada, a.nombre as nombre_autor
            from detalle_pedido d
            join libro l on l.id_libro = d.fid_libro
            join entidad a on a.id_entidad = l.fid_autor
            where d.fid_pedido = ?
            order by d.id_detalle_pedido
        """;
        List<DetallePedido> detalles = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idPedido);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    DetallePedido detalle = new DetallePedido();
                    detalle.setId(rs.getInt("id_detalle_pedido"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                    detalle.setDescuentoUnitario(rs.getDouble("descuento_unitario"));
                    detalle.setSubtotal(rs.getDouble("subtotal"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    Entidad autor = new Entidad();
                    autor.setNombre(rs.getString("nombre_autor"));
                    libro.setAutor(autor);
                    detalle.setLibro(libro);
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    // Una fila por cada libro de cada pedido; las filas de un mismo pedido llegan juntas
    @Override
    public List<Pedido> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = """
            select p.id_pedido, p.estado, p.fecha_compra, p.total,
                   d.cantidad, l.id_libro, l.titulo, l.portada
            from pedido p
            join detalle_pedido d on d.fid_pedido = p.id_pedido
            join libro l on l.id_libro = d.fid_libro
            where p.fid_usuario = ?
            order by p.id_pedido, d.id_detalle_pedido
        """;
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                Pedido pedido = null;
                while (rs.next()) {
                    if (pedido == null || pedido.getId() != rs.getInt("id_pedido")) {
                        pedido = new Pedido();
                        pedido.setId(rs.getInt("id_pedido"));
                        pedido.setEstado(EstadoPedido.valueOf(rs.getString("estado")));
                        pedido.setFechaCompra(rs.getObject("fecha_compra", LocalDateTime.class));
                        pedido.setTotal(rs.getDouble("total"));
                        pedidos.add(pedido);
                    }
                    DetallePedido detalle = new DetallePedido();
                    detalle.setCantidad(rs.getInt("cantidad"));
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setPortada(rs.getString("portada"));
                    detalle.setLibro(libro);
                    pedido.getDetalles().add(detalle);
                }
            }
        }
        return pedidos;
    }

    @Override
    public List<Pedido> listarPorEstado(EstadoPedido estado) throws SQLException {
        String sql = """
            select id_pedido, fid_usuario, fecha_compra, fecha_salida, fecha_entrega, total, contado_para_cupon
            from pedido
            where estado = ?
        """;
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, estado.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Pedido pedido = new Pedido();
                    pedido.setId(rs.getInt("id_pedido"));
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt("fid_usuario"));
                    pedido.setUsuario(usuario);
                    pedido.setEstado(estado);
                    pedido.setFechaCompra(rs.getObject("fecha_compra", LocalDateTime.class));
                    pedido.setFechaSalida(rs.getObject("fecha_salida", LocalDateTime.class));
                    pedido.setFechaEntrega(rs.getObject("fecha_entrega", LocalDateTime.class));
                    pedido.setTotal(rs.getDouble("total"));
                    pedido.setContadoParaCupon(rs.getBoolean("contado_para_cupon"));
                    pedidos.add(pedido);
                }
            }
        }
        return pedidos;
    }

    @Override
    public int contarCancelaciones(int idUsuario, LocalDateTime desde) throws SQLException {
        String sql = "select count(*) from pedido where fid_usuario = ? and fecha_cancelacion >= ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setObject(2, desde);
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int contarCompras(int idUsuario, int idLibro, EstadoPedido estado) throws SQLException {
        String sql = """
            select coalesce(sum(d.cantidad), 0)
            from detalle_pedido d
            join pedido p on p.id_pedido = d.fid_pedido
            where p.fid_usuario = ? and d.fid_libro = ? and p.estado = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idLibro);
            pstm.setString(3, estado.name());
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public List<Venta> listarVentas(List<EstadoPedido> estados, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
            select p.id_pedido, p.fid_usuario, p.fecha_compra, p.total, d.cantidad, d.subtotal,
                   l.id_libro, l.titulo, c.nombre as categoria, a.nombre as autor, e.nombre as editorial
            from detalle_pedido d
            join pedido p on p.id_pedido = d.fid_pedido
            join libro l on l.id_libro = d.fid_libro
            join entidad c on c.id_entidad = l.fid_categoria
            join entidad a on a.id_entidad = l.fid_autor
            join entidad e on e.id_entidad = l.fid_editorial
            where p.estado in (
        """;
        for (int i = 0; i < estados.size(); i++) {
            sql += i == 0 ? "?" : ", ?";
        }
        sql += ")";
        if (desde != null) {
            sql += " and date(p.fecha_compra) >= ?";
        }
        if (hasta != null) {
            sql += " and date(p.fecha_compra) <= ?";
        }
        List<Venta> ventas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            for (EstadoPedido estado : estados) {
                pstm.setString(++i, estado.name());
            }
            if (desde != null) {
                pstm.setObject(++i, desde);
            }
            if (hasta != null) {
                pstm.setObject(++i, hasta);
            }
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Libro libro = new Libro();
                    libro.setId(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    Entidad categoria = new Entidad();
                    categoria.setNombre(rs.getString("categoria"));
                    libro.setCategoria(categoria);
                    Entidad autor = new Entidad();
                    autor.setNombre(rs.getString("autor"));
                    libro.setAutor(autor);
                    Entidad editorial = new Entidad();
                    editorial.setNombre(rs.getString("editorial"));
                    libro.setEditorial(editorial);
                    ventas.add(new Venta(rs.getInt("id_pedido"), rs.getInt("fid_usuario"),
                            rs.getObject("fecha_compra", LocalDateTime.class), rs.getDouble("total"),
                            libro, rs.getInt("cantidad"), rs.getDouble("subtotal")));
                }
            }
        }
        return ventas;
    }

    @Override
    public void registrarSalida(int id, LocalDateTime fecha) throws SQLException {
        String sql = "update pedido set estado = 'EN_CAMINO', fecha_salida = ? where id_pedido = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, fecha);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void registrarEntrega(int id, LocalDateTime fecha) throws SQLException {
        String sql = "update pedido set estado = 'ENTREGADO', fecha_entrega = ? where id_pedido = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, fecha);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public int insertar(Pedido pedido) throws SQLException {
        String sql = """
            insert into pedido (fid_usuario, fid_direccion, fid_metodo_pago, fid_cupon, estado, fecha_compra,
                                codigo_transaccion, codigo_seguimiento, subtotal, descuento_cupon, costo_envio, total,
                                contado_para_cupon)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setInt(1, pedido.getUsuario().getId());
            pstm.setInt(2, pedido.getDireccion().getId());
            pstm.setInt(3, pedido.getMetodoPago().getId());
            if (pedido.getCupon() != null) {
                pstm.setInt(4, pedido.getCupon().getId());
            } else {
                pstm.setNull(4, Types.INTEGER);
            }
            pstm.setString(5, pedido.getEstado().name());
            pstm.setObject(6, pedido.getFechaCompra());
            pstm.setString(7, pedido.getCodigoTransaccion());
            pstm.setString(8, pedido.getCodigoSeguimiento());
            pstm.setDouble(9, pedido.getSubtotal());
            pstm.setDouble(10, pedido.getDescuentoCupon());
            pstm.setDouble(11, pedido.getCostoEnvio());
            pstm.setDouble(12, pedido.getTotal());
            pstm.setBoolean(13, pedido.getContadoParaCupon());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void insertarDetalle(int idPedido, DetallePedido detalle) throws SQLException {
        String sql = """
            insert into detalle_pedido (fid_pedido, fid_libro, cantidad, precio_unitario, descuento_unitario, subtotal)
            values (?, ?, ?, ?, ?, ?)
        """;
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idPedido);
            pstm.setInt(2, detalle.getLibro().getId());
            pstm.setInt(3, detalle.getCantidad());
            pstm.setDouble(4, detalle.getPrecioUnitario());
            pstm.setDouble(5, detalle.getDescuentoUnitario());
            pstm.setDouble(6, detalle.getSubtotal());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cancelar(int id, LocalDateTime fecha) throws SQLException {
        String sql = "update pedido set estado = 'CANCELADO', fecha_cancelacion = ? where id_pedido = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, fecha);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void registrarDevolucion(int id, LocalDateTime fecha) throws SQLException {
        String sql = "update pedido set estado = 'DEVUELTO', fecha_devolucion = ? where id_pedido = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, fecha);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public void marcarContadoParaCupon(int id) throws SQLException {
        String sql = "update pedido set contado_para_cupon = 1 where id_pedido = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        }
    }

    @Override
    public int contarContadosParaCupon(int idUsuario) throws SQLException {
        String sql = "select count(*) from pedido where fid_usuario = ? and contado_para_cupon = 1";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
