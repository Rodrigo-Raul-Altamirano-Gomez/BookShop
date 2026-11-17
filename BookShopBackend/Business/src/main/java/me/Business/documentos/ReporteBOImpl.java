package me.Business.documentos;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.ValidacionException;
import me.DAO.catalogo.LibroDAO;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.usuario.ReporteUsuariosDTO;

public class ReporteBOImpl implements ReporteBO {

    private final LibroDAO libroDAO = new LibroDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final GeneradorPDF generador = new GeneradorPDF();

    private ReporteVentasLibrosDTO prepararLibros(ConsultaReporteLibrosDTO consulta) throws
        NegocioException {
        if (consulta == null || consulta.fechaInicio() == null || consulta.fechaFin() == null)
            throw new ValidacionException("periodo", "Selecciona el inicio y fin del período.");
        if (consulta.fechaFin().isBefore(consulta.fechaInicio()))
            throw new ValidacionException("periodo", "El período no es válido.");
        if (consulta.cantidadMaxima() != null && consulta.cantidadMaxima() < 1)
            throw new ValidacionException("cantidadMaxima",
                "La cantidad maxima debe ser positiva o quedar vacia.");
        try {
            return new ReporteVentasLibrosDTO(consulta.fechaInicio(), consulta.fechaFin(),
                LocalDateTime.now(), consulta.cantidadMaxima(), libroDAO.listarVentasLibros(consulta));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar los datos de ventas.", e);
        }
    }

    private ReporteUsuariosDTO prepararUsuarios(YearMonth mes, Integer cantidadMaxima) throws
        NegocioException {
        if (mes == null)
            throw new ValidacionException("mes", "Selecciona un mes y año válidos.");
        if (cantidadMaxima != null && cantidadMaxima < 1)
            throw new ValidacionException("cantidadMaxima",
                "La cantidad maxima debe ser positiva o quedar vacia.");
        try {
            return new ReporteUsuariosDTO(mes, LocalDateTime.now(), cantidadMaxima,
                usuarioDAO.listarActividadUsuarios(mes.atDay(1), mes.atEndOfMonth(), cantidadMaxima));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron preparar los datos de actividad.", e);
        }
    }

    @Override
    public byte[] generarLibrosPDF(ConsultaReporteLibrosDTO consulta) throws NegocioException {
        return generador.ventasLibros(prepararLibros(consulta));
    }

    @Override
    public byte[] generarUsuariosPDF(YearMonth mes, Integer cantidadMaxima) throws NegocioException {
        return generador.usuarios(prepararUsuarios(mes, cantidadMaxima));
    }

}
