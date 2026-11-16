package me.Business.documentos;

import java.time.YearMonth;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.usuario.ReporteUsuariosDTO;

// Prepara los datos y genera los PDF con las plantillas JRXML (RF18 y RF19).
public interface ReporteBO {

    ReporteVentasLibrosDTO prepararLibros(ConsultaReporteLibrosDTO consulta) throws NegocioException;

    ReporteUsuariosDTO prepararUsuarios(YearMonth mes, Integer cantidadMaxima) throws
        NegocioException;

    // Solo una cuenta administradora (correo @admin.bookshop) puede generar los PDF.
    byte[] generarLibrosPDF(int idActor, ConsultaReporteLibrosDTO consulta) throws NegocioException;

    byte[] generarUsuariosPDF(int idActor, YearMonth mes, Integer cantidadMaxima) throws NegocioException;
}
