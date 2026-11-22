package pe.edu.pucp.bookshop.dao.impl;

import pe.edu.pucp.bookshop.dao.LibroDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.dbmanager.TransactionContext;
import pe.edu.pucp.bookshop.model.Descuento;
import pe.edu.pucp.bookshop.model.Entidad;
import pe.edu.pucp.bookshop.model.Libro;
import pe.edu.pucp.bookshop.model.dto.ConteoLibros;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LibroDAOImpl implements LibroDAO {

    // La usan listarActivos, obtener y listarPaginaAdmin: el libro con los nombres de sus referencias,
    // el descuento de la fecha indicada y el promedio y la cantidad de sus reseñas visibles
    private static final String CONSULTA_LIBRO = """
        select l.id_libro, l.isbn, l.titulo, l.sinopsis, l.idioma, l.paginas, l.anio_publicacion,
               l.precio, l.stock, l.portada, l.activo, l.fecha_registro,
               a.id_entidad as id_autor, a.nombre as nombre_autor,
               c.id_entidad as id_categoria, c.nombre as nombre_categoria,
               e.id_entidad as id_editorial, e.nombre as nombre_editorial,
               d.id_descuento, d.porcentaje, d.fecha_inicio, d.fecha_fin,
               (select avg(r.puntuacion) from resena r where r.fid_libro = l.id_libro and r.activa = 1) as promedio,
               (select count(*) from resena r where r.fid_libro = l.id_libro and r.activa = 1) as cantidad_resenas
        from libro l
        join entidad a on a.id_entidad = l.fid_autor
        join entidad c on c.id_entidad = l.fid_categoria
        join entidad e on e.id_entidad = l.fid_editorial
        left join descuento d on d.fid_libro = l.id_libro and ? between d.fecha_inicio and d.fecha_fin
    """;

    @Override
    public List<Libro> listarActivos(LocalDate hoy) throws SQLException {
        String sql = CONSULTA_LIBRO + " where l.activo = 1";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, hoy);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    libros.add(mapear(rs));
                }
            }
        }
        return libros;
    }

    @Override
    public Libro obtener(int id, LocalDate hoy) throws SQLException {
        String sql = CONSULTA_LIBRO + " where l.id_libro = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setObject(1, hoy);
            pstm.setInt(2, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public int contarAdmin(String texto, Boolean activo) throws SQLException {
        String sql = "select count(*) from libro where 1 = 1";
        if (texto != null) {
            sql += " and titulo like concat('%', ?, '%')";
        }
        if (activo != null) {
            sql += " and activo = ?";
        }
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            if (activo != null) {
                pstm.setBoolean(++i, activo);
            }
            try (ResultSet rs = pstm.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public List<Libro> listarPaginaAdmin(String texto, Boolean activo, int pagina, int tamano, LocalDate hoy) throws SQLException {
        String sql = CONSULTA_LIBRO + " where 1 = 1";
        if (texto != null) {
            sql += " and l.titulo like concat('%', ?, '%')";
        }
        if (activo != null) {
            sql += " and l.activo = ?";
        }
        sql += " order by l.titulo limit ?, ?";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            int i = 0;
            pstm.setObject(++i, hoy);
            if (texto != null) {
                pstm.setString(++i, texto);
            }
            if (activo != null) {
                pstm.setBoolean(++i, activo);
            }
            pstm.setInt(++i, (pagina - 1) * tamano);
            pstm.setInt(++i, tamano);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    libros.add(mapear(rs));
                }
            }
        }
        return libros;
    }

    @Override
    public ConteoLibros contarTodos() throws SQLException {
        String sql = "select count(*), coalesce(sum(activo), 0) from libro";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            rs.next();
            return new ConteoLibros(rs.getInt(1), rs.getInt(2));
        }
    }

    @Override
    public int insertar(Libro libro) throws SQLException {
        String sql = """
            insert into libro (isbn, titulo, sinopsis, idioma, paginas, anio_publicacion, precio, stock, portada, activo,
                               fecha_registro, fid_autor, fid_categoria, fid_editorial)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, libro.getIsbn());
            pstm.setString(2, libro.getTitulo());
            pstm.setString(3, libro.getSinopsis());
            pstm.setString(4, libro.getIdioma());
            pstm.setInt(5, libro.getPaginas());
            pstm.setInt(6, libro.getAnioPublicacion());
            pstm.setDouble(7, libro.getPrecio());
            pstm.setInt(8, libro.getStock());
            pstm.setString(9, libro.getPortada());
            pstm.setBoolean(10, libro.getActivo());
            pstm.setObject(11, libro.getFechaRegistro());
            pstm.setInt(12, libro.getAutor().getId());
            pstm.setInt(13, libro.getCategoria().getId());
            pstm.setInt(14, libro.getEditorial().getId());
            pstm.executeUpdate();
            try (ResultSet rs = pstm.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // RN06: el autor, la categoría y la editorial no se actualizan
    @Override
    public void modificar(Libro libro) throws SQLException {
        String sql = """
            update libro set isbn = ?, titulo = ?, sinopsis = ?, idioma = ?, paginas = ?, anio_publicacion = ?,
                             precio = ?, stock = ?, portada = ?
            where id_libro = ?
        """;
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setString(1, libro.getIsbn());
            pstm.setString(2, libro.getTitulo());
            pstm.setString(3, libro.getSinopsis());
            pstm.setString(4, libro.getIdioma());
            pstm.setInt(5, libro.getPaginas());
            pstm.setInt(6, libro.getAnioPublicacion());
            pstm.setDouble(7, libro.getPrecio());
            pstm.setInt(8, libro.getStock());
            pstm.setString(9, libro.getPortada());
            pstm.setInt(10, libro.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void cambiarActivo(int id, boolean activo) throws SQLException {
        String sql = "update libro set activo = ? where id_libro = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setBoolean(1, activo);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    // Bloquea la fila del libro hasta que termine la transacción: otra compra del mismo libro espera
    @Override
    public void bloquear(int id) throws SQLException {
        String sql = "select id_libro from libro where id_libro = ? for update";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, id);
            pstm.executeQuery().close();
        }
    }

    @Override
    public void descontarStock(int id, int cantidad) throws SQLException {
        String sql = "update libro set stock = stock - ? where id_libro = ?";
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, cantidad);
            pstm.setInt(2, id);
            pstm.executeUpdate();
        }
    }

    // Devuelve al stock las unidades de cada libro del pedido
    @Override
    public void reponerStock(int idPedido) throws SQLException {
        String sql = """
            update libro l
            join detalle_pedido d on d.fid_libro = l.id_libro
            set l.stock = l.stock + d.cantidad
            where d.fid_pedido = ?
        """;
        Connection con = TransactionContext.getConnection();
        try (PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idPedido);
            pstm.executeUpdate();
        }
    }

    private Libro mapear(ResultSet rs) throws SQLException {
        Libro libro = new Libro();
        libro.setId(rs.getInt("id_libro"));
        libro.setIsbn(rs.getString("isbn"));
        libro.setTitulo(rs.getString("titulo"));
        libro.setSinopsis(rs.getString("sinopsis"));
        libro.setIdioma(rs.getString("idioma"));
        libro.setPaginas(rs.getInt("paginas"));
        libro.setAnioPublicacion(rs.getInt("anio_publicacion"));
        libro.setPrecio(rs.getDouble("precio"));
        libro.setStock(rs.getInt("stock"));
        libro.setPortada(rs.getString("portada"));
        libro.setActivo(rs.getBoolean("activo"));
        libro.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));

        Entidad autor = new Entidad();
        autor.setId(rs.getInt("id_autor"));
        autor.setNombre(rs.getString("nombre_autor"));
        libro.setAutor(autor);
        Entidad categoria = new Entidad();
        categoria.setId(rs.getInt("id_categoria"));
        categoria.setNombre(rs.getString("nombre_categoria"));
        libro.setCategoria(categoria);
        Entidad editorial = new Entidad();
        editorial.setId(rs.getInt("id_editorial"));
        editorial.setNombre(rs.getString("nombre_editorial"));
        libro.setEditorial(editorial);

        int idDescuento = rs.getInt("id_descuento");
        if (!rs.wasNull()) {
            Descuento descuento = new Descuento();
            descuento.setId(idDescuento);
            descuento.setPorcentaje(rs.getInt("porcentaje"));
            descuento.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
            descuento.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
            libro.setDescuentoVigente(descuento);
        }
        libro.setPuntuacionPromedio(rs.getDouble("promedio"));
        libro.setCantidadResenas(rs.getInt("cantidad_resenas"));
        return libro;
    }
}
