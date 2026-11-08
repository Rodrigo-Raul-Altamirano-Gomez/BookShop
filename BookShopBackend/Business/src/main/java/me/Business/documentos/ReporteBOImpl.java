package me.Business.documentos;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.usuario.ReporteUsuariosDTO;

public class ReporteBOImpl implements ReporteBO {
    private final LibroDAO libroDAO;
    private final UsuarioDAO usuarioDAO;
    private final Autorizacion autorizacion;
    private final Clock reloj;
    public ReporteBOImpl() {
        this(new LibroDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }
    public ReporteBOImpl(LibroDAO libroDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.libroDAO = libroDAO;
        this.usuarioDAO = usuarioDAO;
        this.autorizacion = new Autorizacion(usuarioDAO);
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }
    @Override
    public ReporteVentasLibrosDTO prepararLibros(int idActor, ConsultaReporteLibrosDTO consulta) throws NegocioException {
        autorizacion.administrador(idActor);
        if (consulta == null || consulta.fechaInicio() == null || consulta.fechaFin() == null)
            throw new ValidacionException("periodo", "Selecciona el inicio y fin del período.");
        if (consulta.fechaFin().isBefore(consulta.fechaInicio()) || consulta.fechaInicio().getYear() < 1000
            || consulta.fechaFin().getYear() > 9998)
            throw new ValidacionException("periodo", "El período no es válido.");
        if (consulta.cantidadMaxima() != null && consulta.cantidadMaxima() < 1)
            throw new ValidacionException("cantidadMaxima", "La cantidad maxima debe ser positiva o quedar vacia.");
        try {
            return new ReporteVentasLibrosDTO(consulta.fechaInicio(), consulta.fechaFin(),
                LocalDateTime.now(reloj), consulta.cantidadMaxima(), libroDAO.listarVentasLibros(consulta));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar los datos de ventas.", e);
        }
    }
    @Override
    public ReporteUsuariosDTO prepararUsuarios(int idActor, YearMonth mes, Integer cantidadMaxima) throws NegocioException {
        autorizacion.administrador(idActor);
        if (mes == null || mes.getYear() < 1000 || mes.getYear() > 9998)
            throw new ValidacionException("mes", "Selecciona un mes y año válidos.");
        if (cantidadMaxima != null && cantidadMaxima < 1)
            throw new ValidacionException("cantidadMaxima", "La cantidad maxima debe ser positiva o quedar vacia.");
        try {
            return new ReporteUsuariosDTO(mes, LocalDateTime.now(reloj), cantidadMaxima,
                usuarioDAO.listarActividadUsuarios(mes.atDay(1), mes.atEndOfMonth(), cantidadMaxima));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar los datos de actividad.", e);
        }
    }
}
