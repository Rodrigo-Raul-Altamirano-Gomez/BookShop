package me.Business.documentos;

import java.time.YearMonth;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;

// Prepara los datos y genera los PDF con las plantillas JRXML (RF18 y RF19).
public interface ReporteBO {

    byte[] generarLibrosPDF(ConsultaReporteLibrosDTO consulta) throws NegocioException;

    byte[] generarUsuariosPDF(YearMonth mes, Integer cantidadMaxima) throws NegocioException;
}
