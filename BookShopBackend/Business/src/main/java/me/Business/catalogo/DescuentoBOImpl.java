package me.Business.catalogo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.Validaciones;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.DescuentoDAOImpl;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.catalogo.DatosDescuentoDTO;
import me.Model.dto.catalogo.DescuentoDTO;
import me.Model.entities.catalogo.Descuento;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;

public class DescuentoBOImpl implements DescuentoBO {
    private final DescuentoDAO descuentos;
    private final LibroDAO libros;
    private final Autorizacion autorizacion;
    private final Clock reloj;

    public DescuentoBOImpl() {
        this(new DescuentoDAOImpl(), new LibroDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public DescuentoBOImpl(DescuentoDAO descuentos, LibroDAO libros, UsuarioDAO usuarios, Clock reloj) {
        this.descuentos = descuentos;
        this.libros = libros;
        this.autorizacion = new Autorizacion(usuarios);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public int guardar(int idActor, int idLibro, DatosDescuentoDTO datos) throws NegocioException {
        Usuario actor = autorizacion.administrador(idActor);
        if (idLibro < 1 || datos == null || datos.id() < 0)
            throw new ValidacionException("descuento", "Seleccione un libro y descuento validos.");
        if (datos.eliminar() && datos.id() == 0) return 0;
        if (!datos.eliminar()) validarDatos(datos);
        try {
            Libro libro = libros.leerParaActualizar(idLibro);
            if (libro == null || libro.isEliminado()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            Descuento descuento;
            if (datos.id() == 0) {
                descuento = new Descuento();
                descuento.setLibro(libro);
                descuento.setUsuarioCreacion(actor.getEmail());
                descuento.setFechaCreacion(LocalDateTime.now(reloj));
            } else {
                descuento = descuentos.leerParaActualizar(datos.id());
                if (descuento == null || descuento.isEliminado()) throw new RecursoNoEncontradoException("El descuento no esta disponible.");
                if (descuento.getLibro().getId() != idLibro) throw new AccesoDenegadoException("El descuento pertenece a otro libro.");
            }
            if (datos.eliminar()) {
                if (!descuentos.eliminar(datos.id(), actor.getEmail())) throw new ConflictoException("El descuento cambio; vuelva a consultarlo.");
            } else {
                if (descuentos.haySuperposicion(idLibro, datos.id(), datos.fechaInicio(), datos.fechaFin()))
                    throw new ValidacionException("fechaInicio", "La vigencia se superpone con otro descuento del libro.");
                descuento.setPorcentaje(datos.porcentaje().setScale(2));
                descuento.setFechaInicio(datos.fechaInicio());
                descuento.setFechaFin(datos.fechaFin());
                LocalDateTime ahora = LocalDateTime.now(reloj);
                descuento.setActivo(!ahora.isBefore(datos.fechaInicio()) && !ahora.isAfter(datos.fechaFin()));
                if (datos.id() == 0) descuentos.insertar(descuento);
                else {
                    descuento.setUsuarioActualizacion(actor.getEmail());
                    descuento.setFechaActualizacion(ahora);
                    descuentos.modificar(descuento);
                }
            }
            TransactionContext.commit();
            return descuento.getId();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo guardar el descuento.", e);
        } catch (NegocioException e) { TransactionContext.rollback(); throw e; }
        finally { TransactionContext.close(); }
    }

    @Override
    public ResultadoPaginadoDTO<DescuentoDTO> listarPorLibro(int idActor, int idLibro, int pagina, int tamanio)
            throws NegocioException {
        autorizacion.administrador(idActor);
        Validaciones.pagina(pagina, tamanio);
        try {
            Libro libro = libros.leer(idLibro);
            if (libro == null || libro.isEliminado()) throw new RecursoNoEncontradoException("El libro no esta disponible.");
            return descuentos.listarPorLibro(idLibro, LocalDateTime.now(reloj), pagina, tamanio);
        } catch (SQLException e) { throw new PersistenciaException("No se pudieron consultar los descuentos.", e); }
    }

    private void validarDatos(DatosDescuentoDTO datos) throws ValidacionException {
        if (datos.porcentaje() == null || datos.porcentaje().signum() <= 0
                || datos.porcentaje().compareTo(BigDecimal.valueOf(100)) > 0 || datos.porcentaje().stripTrailingZeros().scale() > 2)
            throw new ValidacionException("porcentaje", "Ingrese un porcentaje mayor que cero y hasta 100, con dos decimales.");
        if (datos.fechaInicio() == null || datos.fechaFin() == null)
            throw new ValidacionException("vigencia", "Indique ambas fechas de vigencia.");
        if (datos.fechaInicio().getYear() < 1000 || datos.fechaFin().getYear() > 9999 || !datos.fechaFin().isAfter(datos.fechaInicio()))
            throw new ValidacionException("fechaFin", "El fin debe ser posterior al inicio y las fechas deben ser validas.");
    }
}
