package me.DAO.catalogo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import me.Model.dto.ImagenDTO;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.DatosLibroDTO;
import me.Model.dto.catalogo.DetalleLibroDTO;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.GestionLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;
import me.Model.dto.catalogo.ResumenVentasLibroDTO;
import me.Model.dto.catalogo.TarjetaLibroDTO;
import me.Model.dto.catalogo.TarjetaReferenciaDTO;
import me.Model.dto.opinion.ValoracionLibroDTO;
import me.Model.entities.catalogo.Autor;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Editorial;
import me.Model.entities.catalogo.Libro;

public class LibroDAOImpl implements LibroDAO {
    private static final String PRECIO_FINAL = "ROUND(l.precio_base*(100-COALESCE(d.porcentaje,0))/100,2)";
    private static final String COLUMNAS_TARJETA = "l.id,l.titulo,l.portada,l.tipo_portada,a.nombre AS nombre_autor,"
            + "l.precio_base," + PRECIO_FINAL + " AS precio_final,COALESCE(d.porcentaje,0) AS porcentaje,l.activo";

    @Override
    public void insertar(Libro libro) throws SQLException {
        if (libro == null || libro.getAutor() == null || libro.getCategoria() == null || libro.getEditorial() == null) {
            throw new IllegalArgumentException("El libro y sus referencias son obligatorios.");
        }
        Connection conexion = TransactionContext.getConnection();
        // Mantiene disponibles las referencias hasta que el coordinador confirme el alta.
        Autor autor = new AutorDAOImpl().leerParaActualizar(libro.getAutor().getId());
        Categoria categoria = new CategoriaDAOImpl().leerParaActualizar(libro.getCategoria().getId());
        Editorial editorial = new EditorialDAOImpl().leerParaActualizar(libro.getEditorial().getId());
        if (autor == null || !autor.estaDisponible() || categoria == null || !categoria.estaDisponible()
                || editorial == null || !editorial.estaDisponible()) {
            throw new SQLException("Las referencias del libro deben estar disponibles.");
        }
        String sql = "INSERT INTO LIBRO (titulo,idioma,sinopsis,proveedor,isbn,anio_edicion,numero_paginas,"
                + " peso,alto,ancho,saga,numero_tomo,id_autor,id_categoria,id_editorial,precio_base,stock_fisico,"
                + " portada,tipo_portada,numero_vetos,vetado_directamente,usuario_creacion,fecha_creacion)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametrosDatos(sentencia, libro);
            sentencia.setInt(13, libro.getAutor().getId());
            sentencia.setInt(14, libro.getCategoria().getId());
            sentencia.setInt(15, libro.getEditorial().getId());
            sentencia.setBigDecimal(16, libro.getPrecioBase());
            sentencia.setInt(17, libro.getStockFisico());
            sentencia.setBytes(18, libro.getPortada());
            sentencia.setString(19, libro.getTipoPortada());
            sentencia.setInt(20, libro.isVetadoDirectamente() ? 1 : 0);
            sentencia.setBoolean(21, libro.isVetadoDirectamente());
            sentencia.setString(22, libro.getUsuarioCreacion());
            sentencia.setTimestamp(23, Timestamp.valueOf(libro.getFechaCreacion()));
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id del libro.");
                libro.setId(claves.getInt(1));
            }
        }
    }

    @Override
    public void modificar(Libro libro) throws SQLException {
        String sql = "UPDATE LIBRO SET titulo=?,idioma=?,sinopsis=?,proveedor=?,isbn=?,anio_edicion=?,"
                + " numero_paginas=?,peso=?,alto=?,ancho=?,saga=?,numero_tomo=?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametrosDatos(sentencia, libro);
            sentencia.setString(13, libro.getUsuarioActualizacion());
            sentencia.setInt(14, libro.getId());
            sentencia.executeUpdate();
        }
    }

    private void parametrosDatos(PreparedStatement sentencia, Libro libro) throws SQLException {
        sentencia.setString(1, libro.getTitulo());
        sentencia.setString(2, libro.getIdioma());
        sentencia.setString(3, libro.getSinopsis());
        sentencia.setString(4, libro.getProveedor());
        sentencia.setString(5, libro.getIsbn());
        sentencia.setInt(6, libro.getAnioEdicion());
        sentencia.setInt(7, libro.getNumeroPaginas());
        if (libro.getPeso() == null) sentencia.setNull(8, Types.DECIMAL); else sentencia.setDouble(8, libro.getPeso());
        if (libro.getAlto() == null) sentencia.setNull(9, Types.DECIMAL); else sentencia.setDouble(9, libro.getAlto());
        if (libro.getAncho() == null) sentencia.setNull(10, Types.DECIMAL); else sentencia.setDouble(10, libro.getAncho());
        sentencia.setString(11, libro.getSaga());
        if (libro.getNumeroTomo() == null) sentencia.setNull(12, Types.INTEGER); else sentencia.setInt(12, libro.getNumeroTomo());
    }

    @Override
    public Libro leer(int id) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM LIBRO WHERE id=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado, true) : null;
            }
        }
    }

    @Override
    public Libro leerParaActualizar(int id) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM LIBRO WHERE id=? FOR UPDATE")) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado, true) : null;
            }
        }
    }

    @Override
    public boolean eliminar(int id, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO SET eliminado=TRUE,usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP"
                + " WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarImagen(int id, ImagenDTO imagen, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO SET portada=?,tipo_portada=?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBytes(1, imagen == null ? null : imagen.contenido());
            sentencia.setString(2, imagen == null ? null : imagen.tipoMime());
            sentencia.setString(3, usuario);
            sentencia.setInt(4, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarPrecio(int id, BigDecimal precio, String usuario) throws SQLException {
        if (precio == null || precio.signum() <= 0) throw new IllegalArgumentException("El precio debe ser positivo.");
        String sql = "UPDATE LIBRO SET precio_base=?,usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP"
                + " WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, precio);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarStock(int id, int stock, String usuario) throws SQLException {
        if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
        String sql = "UPDATE LIBRO SET stock_fisico=?,usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP"
                + " WHERE id=? AND eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, stock);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean cambiarVetoDirecto(int id, boolean vetado, String usuario) throws SQLException {
        String sql = "UPDATE LIBRO SET vetado_directamente=?,numero_vetos=numero_vetos+?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND eliminado=FALSE AND vetado_directamente<>?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBoolean(1, vetado);
            sentencia.setInt(2, vetado ? 1 : -1);
            sentencia.setString(3, usuario);
            sentencia.setInt(4, id);
            sentencia.setBoolean(5, vetado);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean descontarStock(int id, int cantidad, String usuario) throws SQLException {
        if (cantidad < 1) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE LIBRO SET stock_fisico=stock_fisico-?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND activo=TRUE AND stock_fisico>=?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, cantidad);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            sentencia.setInt(4, cantidad);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public boolean reponerStock(int id, int cantidad, String usuario) throws SQLException {
        if (cantidad < 1) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE LIBRO SET stock_fisico=stock_fisico+?,usuario_actualizacion=?,"
                + " fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=? AND stock_fisico<=?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, cantidad);
            sentencia.setString(2, usuario);
            sentencia.setInt(3, id);
            sentencia.setInt(4, Integer.MAX_VALUE - cantidad);
            return sentencia.executeUpdate() == 1;
        }
    }

    @Override
    public void recalcularVetosPorAutor(int id, String usuario) throws SQLException {
        recalcularVetos("id_autor", id, usuario);
    }

    @Override
    public void recalcularVetosPorCategoria(int id, String usuario) throws SQLException {
        recalcularVetos("id_categoria", id, usuario);
    }

    @Override
    public void recalcularVetosPorEditorial(int id, String usuario) throws SQLException {
        recalcularVetos("id_editorial", id, usuario);
    }

    private void recalcularVetos(String columnaReferencia, int id, String usuario) throws SQLException {
        Connection conexion = TransactionContext.getConnection();
        String sql = "UPDATE LIBRO SET numero_vetos=(CASE WHEN vetado_directamente THEN 1 ELSE 0 END)"
                + " +(CASE WHEN EXISTS(SELECT 1 FROM AUTOR a WHERE a.id=LIBRO.id_autor AND a.activo=TRUE AND a.eliminado=FALSE) THEN 0 ELSE 1 END)"
                + " +(CASE WHEN EXISTS(SELECT 1 FROM CATEGORIA c WHERE c.id=LIBRO.id_categoria AND c.activo=TRUE AND c.eliminado=FALSE) THEN 0 ELSE 1 END)"
                + " +(CASE WHEN EXISTS(SELECT 1 FROM EDITORIAL e WHERE e.id=LIBRO.id_editorial AND e.activo=TRUE AND e.eliminado=FALSE) THEN 0 ELSE 1 END),"
                + " usuario_actualizacion=?,fecha_actualizacion=CURRENT_TIMESTAMP WHERE " + columnaReferencia + "=?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, usuario);
            sentencia.setInt(2, id);
            sentencia.executeUpdate();
        }
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException {
        return buscar(filtro, fecha, 0, 0, 0, 0, false);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorAutor(int id, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException {
        if (id < 1) throw new IllegalArgumentException("El autor debe ser valido.");
        return buscar(filtro, fecha, id, 0, 0, 0, false);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorCategoria(int id, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException {
        if (id < 1) throw new IllegalArgumentException("La categoria debe ser valida.");
        return buscar(filtro, fecha, 0, id, 0, 0, false);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarPorEditorial(int id, FiltroLibroDTO filtro, LocalDateTime fecha) throws SQLException {
        if (id < 1) throw new IllegalArgumentException("La editorial debe ser valida.");
        return buscar(filtro, fecha, 0, 0, id, 0, false);
    }

    @Override
    public ResultadoPaginadoDTO<TarjetaLibroDTO> buscarGuardados(int idUsuario, boolean favorito, FiltroLibroDTO filtro,
            LocalDateTime fecha) throws SQLException {
        if (idUsuario < 1) throw new IllegalArgumentException("El usuario debe ser valido.");
        return buscar(filtro, fecha, 0, 0, 0, idUsuario, favorito);
    }

    private ResultadoPaginadoDTO<TarjetaLibroDTO> buscar(FiltroLibroDTO filtro, LocalDateTime fecha,
            int idAutor, int idCategoria, int idEditorial, int idUsuario, boolean favorito) throws SQLException {
        validarFiltro(filtro, fecha);
        String base = baseTarjetas();
        if (filtro.orden() == OrdenLibro.MAS_VENDIDOS) {
            base += " LEFT JOIN (SELECT i.id_libro,SUM(i.cantidad) AS cantidad FROM ITEM_PEDIDO i"
                    + " JOIN PEDIDO p ON p.id=i.id_pedido WHERE i.eliminado=FALSE AND p.eliminado=FALSE"
                    + " AND p.estado NOT IN ('CANCELADO','DEVUELTO') AND p.fecha_pago>=? AND p.fecha_pago<=?"
                    + " GROUP BY i.id_libro) v ON v.id_libro=l.id";
        }
        String condiciones = condiciones(filtro, idAutor, idCategoria, idEditorial, idUsuario);
        String sql = "SELECT " + COLUMNAS_TARJETA + base + condiciones + " ORDER BY " + orden(filtro.orden()) + " LIMIT ? OFFSET ?";
        List<TarjetaLibroDTO> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement("SELECT COUNT(*)" + base + condiciones)) {
                parametrosFiltro(contar, filtro, fecha, idAutor, idCategoria, idEditorial, idUsuario, favorito);
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = parametrosFiltro(sentencia, filtro, fecha, idAutor, idCategoria, idEditorial, idUsuario, favorito);
                sentencia.setInt(parametro++, filtro.tamanioPagina());
                sentencia.setLong(parametro, (long) (filtro.pagina() - 1) * filtro.tamanioPagina());
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) items.add(mapearTarjeta(resultado));
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, filtro.pagina(), filtro.tamanioPagina());
    }

    private String baseTarjetas() {
        return " FROM LIBRO l JOIN AUTOR a ON a.id=l.id_autor"
                + " LEFT JOIN (SELECT id_libro,MAX(porcentaje) AS porcentaje FROM DESCUENTO WHERE eliminado=FALSE"
                + " AND fecha_inicio<=? AND fecha_fin>=? GROUP BY id_libro) d ON d.id_libro=l.id";
    }

    private String condiciones(FiltroLibroDTO filtro, int idAutor, int idCategoria, int idEditorial, int idUsuario) {
        StringBuilder sql = new StringBuilder(" WHERE l.eliminado=FALSE");
        if (filtro.soloActivos()) sql.append(" AND l.activo=TRUE");
        if (filtro.titulo() != null && !filtro.titulo().isBlank()) sql.append(" AND LOWER(l.titulo) LIKE LOWER(?) ESCAPE '!'");
        if (idAutor > 0) sql.append(" AND l.id_autor=?");
        else if (!filtro.idsAutores().isEmpty()) sql.append(" AND l.id_autor IN (").append(marcadores(filtro.idsAutores().size())).append(')');
        if (idCategoria > 0) sql.append(" AND l.id_categoria=?");
        else if (!filtro.idsCategorias().isEmpty()) sql.append(" AND l.id_categoria IN (").append(marcadores(filtro.idsCategorias().size())).append(')');
        if (idEditorial > 0) sql.append(" AND l.id_editorial=?");
        else if (!filtro.idsEditoriales().isEmpty()) sql.append(" AND l.id_editorial IN (").append(marcadores(filtro.idsEditoriales().size())).append(')');
        if (!filtro.idiomas().isEmpty()) sql.append(" AND l.idioma IN (").append(marcadores(filtro.idiomas().size())).append(')');
        if (filtro.precioMinimo() != null) sql.append(" AND ").append(PRECIO_FINAL).append(">=?");
        if (filtro.precioMaximo() != null) sql.append(" AND ").append(PRECIO_FINAL).append("<=?");
        if (filtro.soloNuevos()) sql.append(" AND l.fecha_creacion>=? AND l.fecha_creacion<=?");
        if (idUsuario > 0) sql.append(" AND EXISTS(SELECT 1 FROM LIBRO_GUARDADO g WHERE g.id_libro=l.id AND g.id_usuario=? AND g.favorito=? AND g.eliminado=FALSE)");
        return sql.toString();
    }

    private int parametrosFiltro(PreparedStatement sentencia, FiltroLibroDTO filtro, LocalDateTime fecha,
            int idAutor, int idCategoria, int idEditorial, int idUsuario, boolean favorito) throws SQLException {
        int parametro = 1;
        sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha));
        sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha));
        if (filtro.orden() == OrdenLibro.MAS_VENDIDOS) {
            sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha.minusDays(30)));
            sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha));
        }
        if (filtro.titulo() != null && !filtro.titulo().isBlank()) sentencia.setString(parametro++, patron(filtro.titulo().trim()));
        if (idAutor > 0) sentencia.setInt(parametro++, idAutor);
        else for (Integer id : filtro.idsAutores()) sentencia.setInt(parametro++, id);
        if (idCategoria > 0) sentencia.setInt(parametro++, idCategoria);
        else for (Integer id : filtro.idsCategorias()) sentencia.setInt(parametro++, id);
        if (idEditorial > 0) sentencia.setInt(parametro++, idEditorial);
        else for (Integer id : filtro.idsEditoriales()) sentencia.setInt(parametro++, id);
        for (String idioma : filtro.idiomas()) sentencia.setString(parametro++, idioma);
        if (filtro.precioMinimo() != null) sentencia.setBigDecimal(parametro++, filtro.precioMinimo());
        if (filtro.precioMaximo() != null) sentencia.setBigDecimal(parametro++, filtro.precioMaximo());
        if (filtro.soloNuevos()) {
            sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha.minusDays(30)));
            sentencia.setTimestamp(parametro++, Timestamp.valueOf(fecha));
        }
        if (idUsuario > 0) {
            sentencia.setInt(parametro++, idUsuario);
            sentencia.setBoolean(parametro++, favorito);
        }
        return parametro;
    }

    private String orden(OrdenLibro orden) {
        return switch (orden) {
            case MAS_RECIENTES -> "l.fecha_creacion DESC,l.id DESC";
            case MAS_VENDIDOS -> "COALESCE(v.cantidad,0) DESC,l.id ASC";
            case PRECIO_ASCENDENTE -> PRECIO_FINAL + " ASC,l.id ASC";
            case PRECIO_DESCENDENTE -> PRECIO_FINAL + " DESC,l.id ASC";
            case TITULO_ASCENDENTE -> "l.titulo ASC,l.id ASC";
            case PAGINAS_ASCENDENTE -> "l.numero_paginas ASC,l.id ASC";
            case PAGINAS_DESCENDENTE -> "l.numero_paginas DESC,l.id ASC";
            case STOCK_DESCENDENTE -> "l.stock_fisico DESC,l.id ASC";
            case ANIO_EDICION_DESCENDENTE -> "l.anio_edicion DESC,l.id ASC";
            case RECOMENDADOS_PRIMERO -> throw new IllegalArgumentException("Business proporciona el orden de recomendaciones.");
        };
    }

    private void validarFiltro(FiltroLibroDTO filtro, LocalDateTime fecha) {
        if (filtro == null || fecha == null || filtro.orden() == null || filtro.orden() == OrdenLibro.RECOMENDADOS_PRIMERO
                || filtro.pagina() < 1 || filtro.tamanioPagina() < 1
                || filtro.idsAutores().stream().anyMatch(id -> id == null || id < 1)
                || filtro.idsCategorias().stream().anyMatch(id -> id == null || id < 1)
                || filtro.idsEditoriales().stream().anyMatch(id -> id == null || id < 1)
                || filtro.idiomas().stream().anyMatch(idioma -> idioma == null || idioma.isBlank())
                || (filtro.precioMinimo() != null && filtro.precioMinimo().signum() < 0)
                || (filtro.precioMaximo() != null && filtro.precioMaximo().signum() < 0)
                || (filtro.precioMinimo() != null && filtro.precioMaximo() != null
                    && filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0)) {
            throw new IllegalArgumentException("Filtro, orden, precios y paginacion deben ser validos.");
        }
    }

    @Override
    public TarjetaLibroDTO obtenerTarjeta(int id, LocalDateTime fecha) throws SQLException {
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        String sql = "SELECT " + COLUMNAS_TARJETA + baseTarjetas() + " WHERE l.id=?";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            sentencia.setInt(3, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapearTarjeta(resultado) : null;
            }
        }
    }

    @Override
    public DetalleLibroDTO obtenerDetalle(int id, int idUsuario, LocalDateTime fecha) throws SQLException {
        if (fecha == null || idUsuario < 0) throw new IllegalArgumentException("Fecha y usuario validos son obligatorios.");
        String sql = "SELECT " + COLUMNAS_TARJETA + ",l.id_autor,l.id_categoria,c.nombre AS nombre_categoria,"
                + " l.id_editorial,e.nombre AS nombre_editorial,l.idioma,l.sinopsis,l.isbn,l.anio_edicion,"
                + " l.numero_paginas,l.peso,l.alto,l.ancho,l.saga,l.numero_tomo,l.stock_fisico,"
                + " EXISTS(SELECT 1 FROM LIBRO_GUARDADO g WHERE g.id_libro=l.id AND g.id_usuario=?"
                + " AND g.favorito=TRUE AND g.eliminado=FALSE) AS favorito,"
                + " EXISTS(SELECT 1 FROM LIBRO_GUARDADO g WHERE g.id_libro=l.id AND g.id_usuario=?"
                + " AND g.favorito=FALSE AND g.eliminado=FALSE) AS deseado,"
                + " COALESCE(r.una,0) AS una,COALESCE(r.dos,0) AS dos,COALESCE(r.tres,0) AS tres,"
                + " COALESCE(r.cuatro,0) AS cuatro,COALESCE(r.cinco,0) AS cinco"
                + baseTarjetas() + " JOIN CATEGORIA c ON c.id=l.id_categoria JOIN EDITORIAL e ON e.id=l.id_editorial"
                + " LEFT JOIN (SELECT id_libro,SUM(CASE WHEN puntuacion=1 THEN 1 ELSE 0 END) AS una,"
                + " SUM(CASE WHEN puntuacion=2 THEN 1 ELSE 0 END) AS dos,SUM(CASE WHEN puntuacion=3 THEN 1 ELSE 0 END) AS tres,"
                + " SUM(CASE WHEN puntuacion=4 THEN 1 ELSE 0 END) AS cuatro,SUM(CASE WHEN puntuacion=5 THEN 1 ELSE 0 END) AS cinco"
                + " FROM RESENA WHERE activo=TRUE AND eliminado=FALSE GROUP BY id_libro) r ON r.id_libro=l.id"
                + " WHERE l.id=? AND l.eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setInt(2, idUsuario);
            sentencia.setTimestamp(3, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(4, Timestamp.valueOf(fecha));
            sentencia.setInt(5, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                ValoracionLibroDTO valoracion = new ValoracionLibroDTO(id, resultado.getLong("una"), resultado.getLong("dos"),
                        resultado.getLong("tres"), resultado.getLong("cuatro"), resultado.getLong("cinco"));
                return new DetalleLibroDTO(mapearTarjeta(resultado), resultado.getInt("id_autor"), resultado.getInt("id_categoria"),
                        resultado.getString("nombre_categoria"), resultado.getInt("id_editorial"), resultado.getString("nombre_editorial"),
                        resultado.getString("idioma"), resultado.getString("sinopsis"), resultado.getString("isbn"),
                        resultado.getInt("anio_edicion"), resultado.getInt("numero_paginas"), decimalOpcional(resultado, "peso"),
                        decimalOpcional(resultado, "alto"), decimalOpcional(resultado, "ancho"), resultado.getString("saga"),
                        enteroOpcional(resultado, "numero_tomo"), resultado.getInt("stock_fisico"), valoracion,
                        resultado.getBoolean("favorito"), resultado.getBoolean("deseado"));
            }
        }
    }

    @Override
    public GestionLibroDTO obtenerGestion(int id) throws SQLException {
        String sql = "SELECT l.*,a.nombre AS nombre_autor,c.nombre AS nombre_categoria,e.nombre AS nombre_editorial"
                + " FROM LIBRO l JOIN AUTOR a ON a.id=l.id_autor JOIN CATEGORIA c ON c.id=l.id_categoria"
                + " JOIN EDITORIAL e ON e.id=l.id_editorial WHERE l.id=? AND l.eliminado=FALSE";
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return null;
                DatosLibroDTO datos = new DatosLibroDTO(resultado.getString("titulo"), resultado.getString("idioma"),
                        resultado.getString("sinopsis"), resultado.getString("proveedor"), resultado.getString("isbn"),
                        resultado.getInt("anio_edicion"), resultado.getInt("numero_paginas"), decimalOpcional(resultado, "peso"),
                        decimalOpcional(resultado, "alto"), decimalOpcional(resultado, "ancho"), resultado.getString("saga"),
                        enteroOpcional(resultado, "numero_tomo"));
                return new GestionLibroDTO(id, datos,
                        new TarjetaReferenciaDTO(resultado.getInt("id_autor"), resultado.getString("nombre_autor"), null),
                        new TarjetaReferenciaDTO(resultado.getInt("id_categoria"), resultado.getString("nombre_categoria"), null),
                        new TarjetaReferenciaDTO(resultado.getInt("id_editorial"), resultado.getString("nombre_editorial"), null),
                        resultado.getBigDecimal("precio_base"), resultado.getInt("stock_fisico"));
            }
        }
    }

    @Override
    public boolean existeIsbn(String isbn, int idExcluir) throws SQLException {
        try (Connection conexion = DBManager.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement("SELECT 1 FROM LIBRO WHERE isbn=? AND id<>? LIMIT 1")) {
            sentencia.setString(1, isbn);
            sentencia.setInt(2, idExcluir);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    @Override
    public ResultadoPaginadoDTO<String> buscarIdiomas(String nombre, boolean soloActivos, int pagina, int tamanioPagina)
            throws SQLException {
        if (pagina < 1 || tamanioPagina < 1) throw new IllegalArgumentException("La paginacion debe ser positiva.");
        String texto = nombre == null ? "" : nombre.trim();
        String condiciones = " WHERE eliminado=FALSE" + (soloActivos ? " AND activo=TRUE" : "")
                + (texto.isEmpty() ? "" : " AND LOWER(idioma) LIKE LOWER(?) ESCAPE '!'");
        List<String> items = new ArrayList<>();
        long total;
        try (Connection conexion = DBManager.getConnection()) {
            try (PreparedStatement contar = conexion.prepareStatement("SELECT COUNT(DISTINCT idioma) FROM LIBRO" + condiciones)) {
                if (!texto.isEmpty()) contar.setString(1, patron(texto));
                try (ResultSet resultado = contar.executeQuery()) {
                    resultado.next();
                    total = resultado.getLong(1);
                }
            }
            String sql = "SELECT DISTINCT idioma FROM LIBRO" + condiciones + " ORDER BY idioma ASC LIMIT ? OFFSET ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                int parametro = 1;
                if (!texto.isEmpty()) sentencia.setString(parametro++, patron(texto));
                sentencia.setInt(parametro++, tamanioPagina);
                sentencia.setLong(parametro, (long) (pagina - 1) * tamanioPagina);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) items.add(resultado.getString("idioma"));
                }
            }
        }
        return new ResultadoPaginadoDTO<>(items, total, pagina, tamanioPagina);
    }

    @Override
    public List<Libro> listarParaRecomendaciones() throws SQLException {
        String sql = "SELECT id,titulo,id_autor,id_categoria,id_editorial,idioma,precio_base,sinopsis,proveedor,isbn,"
                + " anio_edicion,numero_paginas,peso,alto,ancho,saga,numero_tomo,stock_fisico,numero_vetos,"
                + " vetado_directamente,eliminado,usuario_creacion,fecha_creacion,usuario_actualizacion,fecha_actualizacion"
                + " FROM LIBRO WHERE activo=TRUE ORDER BY id";
        List<Libro> libros = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) libros.add(mapear(resultado, false));
        }
        return libros;
    }

    @Override
    public List<TarjetaLibroDTO> listarTarjetasPorIds(List<Integer> ids, LocalDateTime fecha) throws SQLException {
        return listarTarjetas(ids, fecha, true);
    }

    @Override
    public List<TarjetaLibroDTO> listarTarjetasSinImagenPorIds(List<Integer> ids, LocalDateTime fecha) throws SQLException {
        return listarTarjetas(ids, fecha, false);
    }

    private List<TarjetaLibroDTO> listarTarjetas(List<Integer> ids, LocalDateTime fecha, boolean conImagen) throws SQLException {
        if (ids == null || fecha == null || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new IllegalArgumentException("Ids y fecha validos son obligatorios.");
        }
        List<Integer> unicos = new ArrayList<>(new LinkedHashSet<>(ids));
        if (unicos.isEmpty()) return List.of();
        String columnas = conImagen ? COLUMNAS_TARJETA
                : COLUMNAS_TARJETA.replace("l.portada,l.tipo_portada", "NULL AS portada,NULL AS tipo_portada");
        String sql = "SELECT " + columnas + baseTarjetas()
                + " WHERE l.activo=TRUE AND l.id IN (" + marcadores(unicos.size()) + ")";
        Map<Integer, TarjetaLibroDTO> tarjetas = new LinkedHashMap<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(fecha));
            sentencia.setTimestamp(2, Timestamp.valueOf(fecha));
            int parametro = 3;
            for (Integer id : unicos) sentencia.setInt(parametro++, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    TarjetaLibroDTO tarjeta = mapearTarjeta(resultado);
                    tarjetas.put(tarjeta.id(), tarjeta);
                }
            }
        }
        List<TarjetaLibroDTO> ordenadas = new ArrayList<>();
        for (Integer id : unicos) if (tarjetas.containsKey(id)) ordenadas.add(tarjetas.get(id));
        return ordenadas;
    }

    @Override
    public List<ResumenVentasLibroDTO> listarVentasLibros(ConsultaReporteLibrosDTO consulta) throws SQLException {
        if (consulta == null || consulta.fechaInicio() == null || consulta.fechaFin() == null
                || consulta.fechaFin().isBefore(consulta.fechaInicio())
                || (consulta.cantidadMaxima() != null && consulta.cantidadMaxima() < 1)) {
            throw new IllegalArgumentException("Periodo y cantidad maxima deben ser validos.");
        }
        String sql = "SELECT l.id,l.titulo,c.nombre AS categoria,a.nombre AS autor,e.nombre AS editorial,"
                + " SUM(i.cantidad) AS cantidad,SUM(i.sub_total) AS importe FROM ITEM_PEDIDO i"
                + " JOIN PEDIDO p ON p.id=i.id_pedido JOIN LIBRO l ON l.id=i.id_libro"
                + " JOIN CATEGORIA c ON c.id=l.id_categoria JOIN AUTOR a ON a.id=l.id_autor"
                + " JOIN EDITORIAL e ON e.id=l.id_editorial WHERE i.eliminado=FALSE AND p.eliminado=FALSE"
                + " AND p.estado NOT IN ('CANCELADO','DEVUELTO') AND p.fecha_pago>=? AND p.fecha_pago<?"
                + " GROUP BY l.id,l.titulo,c.nombre,a.nombre,e.nombre ORDER BY importe DESC,l.titulo ASC,l.id ASC"
                + (consulta.cantidadMaxima() == null ? "" : " LIMIT ?");
        List<ResumenVentasLibroDTO> ventas = new ArrayList<>();
        try (Connection conexion = DBManager.getConnection(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTimestamp(1, Timestamp.valueOf(consulta.fechaInicio().atStartOfDay()));
            sentencia.setTimestamp(2, Timestamp.valueOf(consulta.fechaFin().plusDays(1).atStartOfDay()));
            if (consulta.cantidadMaxima() != null) sentencia.setInt(3, consulta.cantidadMaxima());
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) ventas.add(new ResumenVentasLibroDTO(resultado.getInt("id"), resultado.getString("titulo"),
                        resultado.getString("categoria"), resultado.getString("autor"), resultado.getString("editorial"),
                        resultado.getLong("cantidad"), resultado.getBigDecimal("importe")));
            }
        }
        return ventas;
    }

    private Libro mapear(ResultSet resultado, boolean conPortada) throws SQLException {
        Libro libro = new Libro();
        libro.setId(resultado.getInt("id"));
        libro.setTitulo(resultado.getString("titulo"));
        Autor autor = new Autor();
        autor.setId(resultado.getInt("id_autor"));
        libro.setAutor(autor);
        Categoria categoria = new Categoria();
        categoria.setId(resultado.getInt("id_categoria"));
        libro.setCategoria(categoria);
        Editorial editorial = new Editorial();
        editorial.setId(resultado.getInt("id_editorial"));
        libro.setEditorial(editorial);
        if (conPortada) {
            libro.setPortada(resultado.getBytes("portada"));
            libro.setTipoPortada(resultado.getString("tipo_portada"));
        }
        libro.setIdioma(resultado.getString("idioma"));
        libro.setPrecioBase(resultado.getBigDecimal("precio_base"));
        libro.setSinopsis(resultado.getString("sinopsis"));
        libro.setProveedor(resultado.getString("proveedor"));
        libro.setIsbn(resultado.getString("isbn"));
        libro.setAnioEdicion(resultado.getInt("anio_edicion"));
        libro.setNumeroPaginas(resultado.getInt("numero_paginas"));
        libro.setPeso(decimalOpcional(resultado, "peso"));
        libro.setAlto(decimalOpcional(resultado, "alto"));
        libro.setAncho(decimalOpcional(resultado, "ancho"));
        libro.setSaga(resultado.getString("saga"));
        libro.setNumeroTomo(enteroOpcional(resultado, "numero_tomo"));
        libro.setStockFisico(resultado.getInt("stock_fisico"));
        libro.setNumeroVetos(resultado.getInt("numero_vetos"));
        libro.setVetadoDirectamente(resultado.getBoolean("vetado_directamente"));
        libro.setEliminado(resultado.getBoolean("eliminado"));
        libro.setUsuarioCreacion(resultado.getString("usuario_creacion"));
        libro.setFechaCreacion(resultado.getTimestamp("fecha_creacion").toLocalDateTime());
        libro.setUsuarioActualizacion(resultado.getString("usuario_actualizacion"));
        Timestamp actualizacion = resultado.getTimestamp("fecha_actualizacion");
        libro.setFechaActualizacion(actualizacion == null ? null : actualizacion.toLocalDateTime());
        return libro;
    }

    private TarjetaLibroDTO mapearTarjeta(ResultSet resultado) throws SQLException {
        byte[] contenido = resultado.getBytes("portada");
        ImagenDTO portada = contenido == null ? null : new ImagenDTO(contenido, resultado.getString("tipo_portada"));
        return new TarjetaLibroDTO(resultado.getInt("id"), resultado.getString("titulo"), portada,
                resultado.getString("nombre_autor"), resultado.getBigDecimal("precio_base"), resultado.getBigDecimal("precio_final").setScale(2),
                resultado.getBigDecimal("porcentaje"), resultado.getBoolean("activo"));
    }

    private Double decimalOpcional(ResultSet resultado, String columna) throws SQLException {
        double valor = resultado.getDouble(columna);
        return resultado.wasNull() ? null : valor;
    }

    private Integer enteroOpcional(ResultSet resultado, String columna) throws SQLException {
        int valor = resultado.getInt(columna);
        return resultado.wasNull() ? null : valor;
    }

    private String marcadores(int cantidad) {
        return String.join(",", Collections.nCopies(cantidad, "?"));
    }

    private String patron(String texto) {
        return "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
